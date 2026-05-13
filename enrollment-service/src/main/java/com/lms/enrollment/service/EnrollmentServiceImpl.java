package com.lms.enrollment.service;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.messaging.EnrollmentEventPublisher;
import com.lms.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentEventPublisher eventPublisher;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Enrollment enroll(int studentId, int courseId, double price) {
        log.info("Attempting to enroll student {} in course {} with price {}", studentId, courseId, price);
        
        Optional<Enrollment> existingEnrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        
        if (existingEnrollment.isPresent()) {
            Enrollment e = existingEnrollment.get();
            if ("Active".equals(e.getStatus()) || "Completed".equals(e.getStatus())) {
                log.info("Student {} already has an active or completed enrollment in course {}. Returning existing.", studentId, courseId);
                return e;
            }
            // If it was cancelled, we can re-activate it
        }

        Enrollment enrollment = existingEnrollment.orElseGet(() -> new Enrollment(studentId, courseId));
        enrollment.setStatus("Active");
        enrollment.setEnrolledAt(LocalDate.now());
        enrollment.setCompletedAt(null);
        enrollment.setProgressPercent(0);
        enrollment.setCertificateIssued(false);
        enrollment.setPriceAtPurchase(price);
        
        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrollment saved for student {} and course {}", studentId, courseId);
        
        // Notify student of successful enrollment
        try {
            String courseServiceUrl = "http://course-service/api/courses/" + courseId;
            java.util.Map<String, Object> course = restTemplate.getForObject(courseServiceUrl, java.util.Map.class);
            String courseTitle = (course != null && course.get("title") != null) ? course.get("title").toString() : "Course #" + courseId;
            
            eventPublisher.publishEnrollmentSuccess(saved, courseTitle);

            // Notify instructor
            if (course != null && course.get("instructorId") != null) {
                int instructorId = Integer.parseInt(course.get("instructorId").toString());
                
                // Fetch student name from auth-service
                String studentName = "A new student";
                try {
                    String authServiceUrl = "http://auth-service/auth/profile/" + studentId;
                    java.util.Map<String, Object> student = restTemplate.getForObject(authServiceUrl, java.util.Map.class);
                    if (student != null && student.get("fullName") != null) {
                        studentName = student.get("fullName").toString();
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch student name for notification: {}", e.getMessage());
                }
                
                eventPublisher.publishNewEnrollmentAlert(instructorId, studentId, courseId, studentName, courseTitle);
            }
        } catch (Exception e) {
            log.warn("Failed to publish enrollment notifications: {}", e.getMessage());
        }
        
        return saved;
    }

    @Override
    public Enrollment enroll(int studentId, int courseId) {
        return enroll(studentId, courseId, 0.0);
    }

    @Override
    public void unenroll(int studentId, int courseId) {
        Enrollment enrollment = getActiveEnrollment(studentId, courseId);
        enrollment.setStatus("Cancelled");
        enrollmentRepository.save(enrollment);
    }

    @Override
    public List<Enrollment> getEnrollmentsByStudent(int studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Override
    public List<Enrollment> getEnrollmentsByCourse(int courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    @Override
    public void updateProgress(int studentId, int courseId, int progressPercent) {
        if (progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("Progress percent must be between 0 and 100");
        }
        Enrollment enrollment = getActiveEnrollment(studentId, courseId);
        
        boolean justCompleted = false;
        if (progressPercent == 100 && !"Completed".equals(enrollment.getStatus())) {
            justCompleted = true;
            enrollment.setStatus("Completed");
            enrollment.setCompletedAt(LocalDate.now());
        }
        
        enrollment.setProgressPercent(progressPercent);
        enrollmentRepository.save(enrollment);
        
        if (justCompleted) {
            String courseTitle = "your course";
            try {
                String courseServiceUrl = "http://course-service/api/courses/" + courseId;
                java.util.Map<String, Object> course = restTemplate.getForObject(courseServiceUrl, java.util.Map.class);
                if (course != null && course.get("title") != null) {
                    courseTitle = course.get("title").toString();
                }
            } catch (Exception e) {
                log.warn("Could not fetch course title for completion notification: {}", e.getMessage());
            }
            eventPublisher.publishCourseCompletion(enrollment, courseTitle);
        }
    }

    @Override
    public void markComplete(int studentId, int courseId) {
        Enrollment enrollment = getActiveEnrollment(studentId, courseId);
        if (!"Completed".equals(enrollment.getStatus())) {
            enrollment.setStatus("Completed");
            enrollment.setProgressPercent(100);
            enrollment.setCompletedAt(LocalDate.now());
            enrollmentRepository.save(enrollment);
            
            String courseTitle = "your course";
            try {
                String courseServiceUrl = "http://course-service/api/courses/" + courseId;
                java.util.Map<String, Object> course = restTemplate.getForObject(courseServiceUrl, java.util.Map.class);
                if (course != null && course.get("title") != null) {
                    courseTitle = course.get("title").toString();
                }
            } catch (Exception e) {
                log.warn("Could not fetch course title for completion notification: {}", e.getMessage());
            }
            eventPublisher.publishCourseCompletion(enrollment, courseTitle);
        }
    }

    @Override
    public boolean isEnrolled(int studentId, int courseId) {
        return enrollmentRepository.existsActiveByStudentIdAndCourseId(studentId, courseId)
                || enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                        .map(enrollment -> !"Cancelled".equals(enrollment.getStatus()))
                        .orElseGet(() -> enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId));
    }

    @Override
    public void issueCertificate(int studentId, int courseId) {
        Enrollment enrollment = getActiveEnrollment(studentId, courseId);
        if (!"Completed".equals(enrollment.getStatus())) {
            throw new IllegalStateException("Certificate can only be issued for completed enrollments");
        }
        if (enrollment.isCertificateIssued()) {
            throw new IllegalStateException("Certificate has already been issued for this enrollment");
        }
        enrollment.setCertificateIssued(true);
        enrollmentRepository.save(enrollment);
    }

    @Override
    public int getEnrollmentCount(int courseId) {
        int activeCount = enrollmentRepository.countActiveByCourseId(courseId);
        return activeCount > 0 ? activeCount : enrollmentRepository.countByCourseId(courseId);
    }

    @Override
    public Map<Integer, Integer> getEnrollmentCounts(List<Integer> courseIds) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Integer courseId : courseIds) {
            if (courseId != null) {
                counts.put(courseId, getEnrollmentCount(courseId));
            }
        }
        return counts;
    }

    @Override
    public java.util.Map<String, Object> getAdminStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalEnrollments", enrollmentRepository.count());
        stats.put("activeEnrollments", enrollmentRepository.countByStatus("Active"));
        stats.put("completedEnrollments", enrollmentRepository.countByStatus("Completed"));
        return stats;
    }

    private Enrollment getActiveEnrollment(int studentId, int courseId) {
        return enrollmentRepository.findActiveByStudentIdAndCourseId(studentId, courseId)
                .or(() -> enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                        .filter(enrollment -> !"Cancelled".equals(enrollment.getStatus())))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No enrollment found for student " + studentId + " and course " + courseId));
    }
}
