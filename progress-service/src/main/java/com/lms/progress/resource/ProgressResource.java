package com.lms.progress.resource;

import com.lms.progress.dto.ApiResponse;
import com.lms.progress.entity.Certificate;
import com.lms.progress.entity.Progress;
import com.lms.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing all progress-tracking and certificate endpoints.
 *
 * Base paths:
 *   /progress   – lesson-level and course-level progress operations
 *   /certificates – certificate issuance, retrieval, and verification
 *
 * Security note:
 *   All endpoints require a valid JWT except GET /certificates/verify,
 *   which is public so third parties can authenticate credentials.
 */
@RestController
@RequestMapping({"/api/progress", "/progress"})
@Tag(name = "Progress & Certificates", description = "Track learning activity and manage completion certificates")
public class ProgressResource {

    private static final Logger log = LoggerFactory.getLogger(ProgressResource.class);

    private final ProgressService progressService;

    public ProgressResource(ProgressService progressService) {
        this.progressService = progressService;
    }

    /* ================================================================== */
    /* Progress Endpoints                                                  */
    /* ================================================================== */

    /**
     * Records or accumulates seconds watched for a lesson.
     *
     * POST /progress/track?studentId=1&courseId=2&lessonId=3&watchedSeconds=120
     */
    @PostMapping("/track")
    @Operation(summary = "Track lesson progress", description = "Accumulates watched seconds for a lesson")
    public ResponseEntity<ApiResponse<String>> track(
            @Parameter(description = "Student ID") @RequestParam int studentId,
            @Parameter(description = "Course ID")  @RequestParam int courseId,
            @Parameter(description = "Lesson ID")  @RequestParam int lessonId,
            @Parameter(description = "Seconds watched in this session") @RequestParam int watchedSeconds) {

        log.info("POST /progress/track | studentId={} courseId={} lessonId={} watchedSeconds={}",
                studentId, courseId, lessonId, watchedSeconds);

        if (watchedSeconds < 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("watchedSeconds must be >= 0"));
        }

        progressService.trackProgress(studentId, courseId, lessonId, watchedSeconds);
        return ResponseEntity.ok(ApiResponse.success("Progress tracked successfully", null));
    }

    /**
     * Marks a lesson as fully completed.
     *
     * POST /progress/complete?studentId=1&courseId=2&lessonId=3
     */
    @PostMapping("/complete")
    @Operation(summary = "Mark lesson complete", description = "Sets isCompleted=true and records completedAt")
    public ResponseEntity<ApiResponse<String>> markComplete(
            @RequestParam int studentId,
            @RequestParam int courseId,
            @RequestParam int lessonId) {

        log.info("POST /progress/complete | studentId={} courseId={} lessonId={}",
                studentId, courseId, lessonId);

        progressService.markLessonComplete(studentId, courseId, lessonId);
        return ResponseEntity.ok(ApiResponse.success("Lesson marked as complete", null));
    }

    /**
     * Returns the course-level completion percentage [0–100].
     *
     * GET /progress/course?studentId=1&courseId=2
     */
    @GetMapping("/course")
    @Operation(summary = "Get course completion %", description = "Returns integer completion percentage for a course")
    public ResponseEntity<ApiResponse<Integer>> getCourseProgress(
            @RequestParam int studentId,
            @RequestParam int courseId) {

        log.info("GET /progress/course | studentId={} courseId={}", studentId, courseId);

        int percentage = progressService.getCourseProgress(studentId, courseId);
        return ResponseEntity.ok(ApiResponse.success("Course progress retrieved", percentage));
    }

    /**
     * Returns the progress record for a specific lesson.
     *
     * GET /progress/lesson?studentId=1&lessonId=3
     */
    @GetMapping("/lesson")
    @Operation(summary = "Get lesson progress", description = "Returns the progress record for a specific lesson")
    public ResponseEntity<ApiResponse<Progress>> getLessonProgress(
            @RequestParam int studentId,
            @RequestParam int lessonId) {

        log.info("GET /progress/lesson | studentId={} lessonId={}", studentId, lessonId);

        return progressService.getLessonProgress(studentId, lessonId)
                .map(p -> ResponseEntity.ok(ApiResponse.success("Lesson progress retrieved", p)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("No progress found for this lesson")));
    }

    /**
     * Returns all progress records for a student across all courses.
     *
     * GET /progress/student?studentId=1
     */
    @GetMapping("/student")
    @Operation(summary = "Get all progress for student", description = "Returns all lesson-level progress records")
    public ResponseEntity<ApiResponse<List<Progress>>> getAllProgressByStudent(@RequestParam int studentId) {

        log.info("GET /progress/student | studentId={}", studentId);

        List<Progress> list = progressService.getAllProgressByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success("Progress records retrieved", list));
    }


    /* ================================================================== */
    /* Certificate Endpoints                                               */
    /* ================================================================== */

    /**
     * Issues a certificate when the student has completed 100% of a course.
     *
     * POST /certificates/issue?studentId=1&courseId=2
     */
    @PostMapping("/certificates/issue")
    @Operation(summary = "Issue certificate", description = "Issues a certificate if course is 100% complete")
    public ResponseEntity<ApiResponse<Certificate>> issueCertificate(
            @RequestParam int studentId,
            @RequestParam int courseId,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String instructorName,
            @RequestParam(required = false) String courseLevel,
            @RequestParam(required = false) Integer courseDuration) {

        log.info("POST /certificates/issue | studentId={} courseId={} courseName={}", studentId, courseId, courseName);

        Certificate cert = progressService.issueCertificate(studentId, courseId, courseName, instructorName, courseLevel, courseDuration);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Certificate issued successfully", cert));
    }

    /**
     * Retrieves a previously issued certificate for a student/course pair.
     *
     * GET /certificates?studentId=1&courseId=2
     */
    @GetMapping("/certificates")
    @Operation(summary = "Get certificate", description = "Retrieves the certificate for a student/course pair")
    public ResponseEntity<ApiResponse<Certificate>> getCertificate(
            @RequestParam int studentId,
            @RequestParam int courseId) {

        log.info("GET /certificates | studentId={} courseId={}", studentId, courseId);

        return progressService.getCertificate(studentId, courseId)
                .map(c -> ResponseEntity.ok(ApiResponse.success("Certificate retrieved", c)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("No certificate found for this student/course")));
    }

    /**
     * Retrieves all certificates earned by a student.
     *
     * GET /certificates/student?studentId=1
     */
    @GetMapping("/certificates/student")
    @Operation(summary = "Get all student certificates", description = "Returns all certificates earned by a student")
    public ResponseEntity<ApiResponse<List<Certificate>>> getStudentCertificates(@RequestParam int studentId) {

        log.info("GET /certificates/student | studentId={}", studentId);

        List<Certificate> list = progressService.getCertificatesByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success("Certificates retrieved successfully", list));
    }

    /**
     * Public endpoint – verifies a certificate by its unique verification code.
     * Does NOT require JWT authentication.
     *
     * GET /certificates/verify?code=UUID
     */
    @GetMapping("/certificates/verify")
    @Operation(summary = "Verify certificate", description = "Public endpoint: validates a certificate by its unique code")
    public ResponseEntity<ApiResponse<Certificate>> verifyCertificate(
            @RequestParam String code) {

        log.info("GET /certificates/verify | code={}", code);

        Certificate cert = progressService.verifyCertificate(code);
        return ResponseEntity.ok(ApiResponse.success("Certificate is valid", cert));
    }
}
