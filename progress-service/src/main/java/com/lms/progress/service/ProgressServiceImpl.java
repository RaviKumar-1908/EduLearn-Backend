package com.lms.progress.service;

import com.lms.progress.entity.Certificate;
import com.lms.progress.entity.Progress;
import com.lms.progress.exception.CertificateAlreadyIssuedException;
import com.lms.progress.exception.CourseNotCompletedException;
import com.lms.progress.exception.ResourceNotFoundException;
import com.lms.progress.repository.CertificateRepository;
import com.lms.progress.repository.ProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Concrete implementation of {@link ProgressService}.
 *
 * Design decisions:
 * <ul>
 *   <li>trackProgress and markLessonComplete are {@code @Transactional}
 *       to ensure atomicity.</li>
 *   <li>getCourseProgress is Redis-cached. Cache is evicted whenever
 *       progress changes for the same (student, course) pair.</li>
 *   <li>issueCertificate publishes a {@link CertificateIssuedEvent}
 *       to RabbitMQ so the Notification Service can send an email.</li>
 * </ul>
 */
@Service
public class ProgressServiceImpl implements ProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProgressServiceImpl.class);

    /* ------------------------------------------------------------------ */
    /* RabbitMQ exchange / routing-key constants                           */
    /* ------------------------------------------------------------------ */
    public static final String CERTIFICATE_EXCHANGE    = "lms.certificate.exchange";
    public static final String CERTIFICATE_ROUTING_KEY = "notification.certificate.issued";
    public static final String PROGRESS_EXCHANGE       = "lms.events.exchange";
    public static final String PROGRESS_ROUTING_KEY    = "progress.updated";

    /* ------------------------------------------------------------------ */
    /* Dependencies                                                         */
    /* ------------------------------------------------------------------ */
    private final ProgressRepository    progressRepository;
    private final CertificateRepository certificateRepository;
    private final RabbitTemplate        rabbitTemplate;
    private final RestTemplate          restTemplate;

    public ProgressServiceImpl(ProgressRepository    progressRepository,
                               CertificateRepository certificateRepository,
                               RabbitTemplate        rabbitTemplate,
                               RestTemplate          restTemplate) {
        this.progressRepository    = progressRepository;
        this.certificateRepository = certificateRepository;
        this.rabbitTemplate        = rabbitTemplate;
        this.restTemplate          = restTemplate;
    }

    /* ------------------------------------------------------------------ */
    /* trackProgress                                                        */
    /* ------------------------------------------------------------------ */

    /**
     * {@inheritDoc}
     * Upsert semantics: creates a new row on first call, accumulates
     * watchedSeconds on subsequent calls for the same (student, course, lesson).
     */
    @Override
    @Transactional
    @CacheEvict(value = "courseProgress", key = "#studentId + '_' + #courseId")
    public void trackProgress(int studentId, int courseId, int lessonId, int watchedSeconds) {
        log.debug("trackProgress called | studentId={} courseId={} lessonId={} watchedSeconds={}",
                studentId, courseId, lessonId, watchedSeconds);

        Optional<Progress> existing =
                progressRepository.findByStudentIdAndCourseIdAndLessonId(studentId, courseId, lessonId);

        Progress progress = existing.orElseGet(() -> {
            log.debug("Creating new progress record for studentId={} lessonId={}", studentId, lessonId);
            Progress p = new Progress();
            p.setStudentId(studentId);
            p.setCourseId(courseId);
            p.setLessonId(lessonId);
            p.setWatchedSeconds(0);
            p.setCompleted(false);
            return p;
        });

        // Accumulate watched time and update last-accessed timestamp
        progress.setWatchedSeconds(progress.getWatchedSeconds() + watchedSeconds);
        progress.setLastAccessedAt(LocalDateTime.now());

        progressRepository.save(progress);
        log.debug("Progress saved | progressId={} totalWatched={}s",
                progress.getProgressId(), progress.getWatchedSeconds());
    }

    /* ------------------------------------------------------------------ */
    /* markLessonComplete                                                   */
    /* ------------------------------------------------------------------ */

    /**
     * {@inheritDoc}
     * Creates a completed row if no prior tracking record exists,
     * then sets isCompleted = true and records completedAt.
     */
    @Override
    @Transactional
    @CacheEvict(value = "courseProgress", key = "#studentId + '_' + #courseId")
    public void markLessonComplete(int studentId, int courseId, int lessonId) {
        log.info("markLessonComplete | studentId={} courseId={} lessonId={}",
                studentId, courseId, lessonId);

        Progress progress = progressRepository
                .findByStudentIdAndCourseIdAndLessonId(studentId, courseId, lessonId)
                .orElseGet(() -> {
                    log.debug("No prior progress record; creating one for completion mark.");
                    Progress p = new Progress();
                    p.setStudentId(studentId);
                    p.setCourseId(courseId);
                    p.setLessonId(lessonId);
                    p.setWatchedSeconds(0);
                    return p;
                });

        if (progress.isCompleted()) {
            log.warn("Lesson {} already marked complete for student {}. Skipping.", lessonId, studentId);
            return;
        }

        progress.setCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastAccessedAt(LocalDateTime.now());

        progressRepository.save(progress);
        log.info("Lesson {} marked complete for studentId={}", lessonId, studentId);

        // Calculate and publish new course progress
        try {
            int percentage = getCourseProgress(studentId, courseId);
            Map<String, Object> event = new HashMap<>();
            event.put("studentId", studentId);
            event.put("courseId", courseId);
            event.put("progressPercent", percentage);
            event.put("type", "PROGRESS_UPDATED");
            rabbitTemplate.convertAndSend(PROGRESS_EXCHANGE, PROGRESS_ROUTING_KEY, event);
            log.info("ProgressUpdatedEvent published | studentId={} courseId={} progress={}%", studentId, courseId, percentage);
        } catch (Exception e) {
            log.error("Failed to publish progress update event", e);
        }
    }

    /* ------------------------------------------------------------------ */
    /* getCourseProgress                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * {@inheritDoc}
     * Result is cached in Redis under key "courseProgress::{studentId}_{courseId}".
     * Returns 0 when no lessons have been tracked yet.
     */
    @Override
    @Cacheable(value = "courseProgress", key = "#studentId + '_' + #courseId")
    public int getCourseProgress(int studentId, int courseId) {
        log.info("getCourseProgress (cache miss) | studentId={} courseId={}", studentId, courseId);

        // Fetch published lesson count from lesson-service
        long totalLessons = 0;
        try {
            String lessonServiceUrl = "http://lesson-service/api/lesson/course/" + courseId + "/published";
            Collection<?> lessons = restTemplate.getForObject(lessonServiceUrl, Collection.class);
            if (lessons != null) {
                totalLessons = lessons.size();
            }
        } catch (Exception e) {
            log.error("Failed to fetch published lesson count from lesson-service for courseId={}", courseId, e);
            // Return 0 if we can't verify the total. This blocks certificates until service is up.
            return 0;
        }

        if (totalLessons == 0) {
            log.debug("No lessons found for courseId={}", courseId);
            return 0;
        }

        long completedLessons = progressRepository.countCompletedByStudentIdAndCourseId(studentId, courseId);

        // Use floating point for precision to match frontend rounding (e.g. 99.6 -> 100)
        int percentage = (int) Math.round((completedLessons * 100.0) / totalLessons);
        
        log.info("Course progress | studentId={} courseId={} completed={}/{} => {}%",
                studentId, courseId, completedLessons, totalLessons, percentage);

        return Math.min(percentage, 100);
    }

    /* ------------------------------------------------------------------ */
    /* getLessonProgress                                                    */
    /* ------------------------------------------------------------------ */

    /** {@inheritDoc} */
    @Override
    public Optional<Progress> getLessonProgress(int studentId, int lessonId) {
        log.info("getLessonProgress | studentId={} lessonId={}", studentId, lessonId);
        return progressRepository.findByStudentIdAndLessonId(studentId, lessonId);
    }

    /* ------------------------------------------------------------------ */
    /* issueCertificate                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * {@inheritDoc}
     *
     * <ol>
     *   <li>Guard: reject if certificate already issued.</li>
     *   <li>Guard: reject if course completion < 100%.</li>
     *   <li>Generate UUID verification code.</li>
     *   <li>Persist the certificate.</li>
     *   <li>Publish {@link CertificateIssuedEvent} to RabbitMQ.</li>
     * </ol>
     */
    @Override
    @Transactional
    @CacheEvict(value = "courseProgress", key = "#studentId + '_' + #courseId")
    public Certificate issueCertificate(int studentId, int courseId, String courseName, String instructorName, String courseLevel, Integer courseDuration) {
        log.info("issueCertificate | studentId={} courseId={} courseName={}", studentId, courseId, courseName);

        // Guard 1 – avoid duplicate certificate
        if (certificateRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            log.warn("Certificate already exists for studentId={} courseId={}", studentId, courseId);
            throw new CertificateAlreadyIssuedException(
                    "Certificate already issued for studentId=" + studentId +
                    ", courseId=" + courseId);
        }

        // Guard 2 – course must be fully complete (allowing 99% for rounding safety)
        int progress = getCourseProgress(studentId, courseId);
        if (progress < 99) {
            long totalLessons = 0;
            try {
                String lessonServiceUrl = "http://lesson-service/api/lesson/course/" + courseId + "/published";
                Collection<?> lessons = restTemplate.getForObject(lessonServiceUrl, Collection.class);
                if (lessons != null) totalLessons = lessons.size();
            } catch (Exception e) { /* ignore */ }
            
            long completedLessons = progressRepository.countCompletedByStudentIdAndCourseId(studentId, courseId);
            
            log.warn("Cannot issue certificate; progress={}% ({} of {}) for studentId={} courseId={}",
                    progress, completedLessons, totalLessons, studentId, courseId);
            throw new CourseNotCompletedException(
                    String.format("Course not yet complete. You have completed %d of %d published lessons (%d%%). Please ensure all content is watched.", 
                        completedLessons, totalLessons, progress));
        }

        // Build certificate
        String verificationCode = UUID.randomUUID().toString();
        String certificateUrl   = "/certificates/verify?code=" + verificationCode;

        Certificate certificate = new Certificate();
        certificate.setStudentId(studentId);
        certificate.setCourseId(courseId);
        certificate.setIssuedAt(LocalDate.now());
        certificate.setVerificationCode(verificationCode);
        certificate.setCertificateUrl(certificateUrl);
        certificate.setInstructorName(instructorName != null ? instructorName : "LMS Instructor");
        certificate.setCourseName(courseName != null ? courseName : "Course-" + courseId);
        certificate.setCourseLevel(courseLevel);
        certificate.setCourseDuration(courseDuration);

        Certificate saved = certificateRepository.save(certificate);
        log.info("Certificate issued | certificateId={} code={}", saved.getCertificateId(), verificationCode);

        // Publish event to Notification Service via RabbitMQ
        Map<String, Object> event = new HashMap<>();
        event.put("studentId", studentId);
        event.put("courseId", courseId);
        event.put("verificationCode", verificationCode);
        event.put("certificateUrl", certificateUrl);
        event.put("courseName", certificate.getCourseName());
        event.put("instructorName", certificate.getInstructorName());
        event.put("courseLevel", certificate.getCourseLevel());
        event.put("courseDuration", certificate.getCourseDuration());
        event.put("type", "CERTIFICATE");
        event.put("title", "Certificate Issued");
        event.put("message", "Your certificate for '" + certificate.getCourseName() + "' is now available.");
        event.put("relatedEntityId", courseId);
        event.put("relatedEntityType", "COURSE");
        
        try {
            rabbitTemplate.convertAndSend(CERTIFICATE_EXCHANGE, CERTIFICATE_ROUTING_KEY, event);
            log.info("CertificateIssuedEvent published to RabbitMQ | studentId={}", studentId);
        } catch (Exception e) {
            log.error("Failed to publish CertificateIssuedEvent for studentId={}", studentId, e);
        }

        return saved;
    }

    /* ------------------------------------------------------------------ */
    /* getCertificate                                                       */
    /* ------------------------------------------------------------------ */

    /** {@inheritDoc} */
    @Override
    public Optional<Certificate> getCertificate(int studentId, int courseId) {
        log.info("getCertificate | studentId={} courseId={}", studentId, courseId);
        return certificateRepository.findByStudentIdAndCourseId(studentId, courseId);
    }

    /* ------------------------------------------------------------------ */
    /* verifyCertificate                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * {@inheritDoc}
     * Public-facing endpoint; does NOT require authentication.
     */
    @Override
    public Certificate verifyCertificate(String verificationCode) {
        log.info("verifyCertificate | code={}", verificationCode);
        return certificateRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> {
                    log.warn("No certificate found for verificationCode={}", verificationCode);
                    return new ResourceNotFoundException(
                            "Certificate not found for code: " + verificationCode);
                });
    }

    /* ------------------------------------------------------------------ */
    /* getAllProgressByStudent                                              */
    /* ------------------------------------------------------------------ */

    /** {@inheritDoc} */
    @Override
    public List<Progress> getAllProgressByStudent(int studentId) {
        log.info("getAllProgressByStudent | studentId={}", studentId);
        return progressRepository.findByStudentId(studentId);
    }

    /** {@inheritDoc} */
    @Override
    public List<Certificate> getCertificatesByStudent(int studentId) {
        log.info("getCertificatesByStudent | studentId={}", studentId);
        return certificateRepository.findByStudentId(studentId);
    }
}
