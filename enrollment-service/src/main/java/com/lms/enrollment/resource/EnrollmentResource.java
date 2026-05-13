package com.lms.enrollment.resource;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({ "/api/enrollment", "/api/enrollments", "/enrollments" })
public class EnrollmentResource {

    @Autowired
    private EnrollmentService enrollmentService;

    public EnrollmentResource() {
    }

    // POST /enrollments/enroll?studentId=1&courseId=2
    @PostMapping("/enroll")
    public ResponseEntity<Enrollment> enroll(
            @RequestParam int studentId,
            @RequestParam int courseId,
            @RequestParam(defaultValue = "0.0") double price) {
        try {
            Enrollment enrollment = enrollmentService.enroll(studentId, courseId, price);
            return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // DELETE /enrollments/unenroll?studentId=1&courseId=2
    @DeleteMapping("/unenroll")
    public ResponseEntity<Void> unenroll(
            @RequestParam int studentId,
            @RequestParam int courseId) {
        try {
            enrollmentService.unenroll(studentId, courseId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /enrollments/student/{studentId}
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Enrollment>> getByStudent(@PathVariable int studentId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByStudent(studentId);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping({ "/dashboard/student/{studentId}", "/student-dashboard/{studentId}" })
    public ResponseEntity<List<Enrollment>> getStudentDashboard(@PathVariable int studentId) {
        return getByStudent(studentId);
    }

    // GET /enrollments/course/{courseId}
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Enrollment>> getByCourse(@PathVariable int courseId) {
        List<Enrollment> enrollments = enrollmentService.getEnrollmentsByCourse(courseId);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping({ "/dashboard/course/{courseId}", "/instructor-dashboard/course/{courseId}" })
    public ResponseEntity<List<Enrollment>> getCourseDashboard(@PathVariable int courseId) {
        return getByCourse(courseId);
    }

    // PUT /enrollments/progress?studentId=1&courseId=2&progressPercent=75
    @PutMapping("/progress")
    public ResponseEntity<Void> updateProgress(
            @RequestParam int studentId,
            @RequestParam int courseId,
            @RequestParam int progressPercent) {
        try {
            enrollmentService.updateProgress(studentId, courseId, progressPercent);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /enrollments/complete?studentId=1&courseId=2
    @PutMapping("/complete")
    public ResponseEntity<Void> markComplete(
            @RequestParam int studentId,
            @RequestParam int courseId) {
        try {
            enrollmentService.markComplete(studentId, courseId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /enrollments/isEnrolled?studentId=1&courseId=2
    @GetMapping("/isEnrolled")
    public ResponseEntity<Boolean> isEnrolled(
            @RequestParam int studentId,
            @RequestParam int courseId) {
        boolean enrolled = enrollmentService.isEnrolled(studentId, courseId);
        return ResponseEntity.ok(enrolled);
    }

    // POST /enrollments/certificate?studentId=1&courseId=2
    @PostMapping("/certificate")
    public ResponseEntity<Void> issueCertificate(
            @RequestParam int studentId,
            @RequestParam int courseId) {
        try {
            enrollmentService.issueCertificate(studentId, courseId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /enrollments/count/{courseId}
    @GetMapping("/count/{courseId}")
    public ResponseEntity<Integer> getEnrollmentCount(@PathVariable int courseId) {
        int count = enrollmentService.getEnrollmentCount(courseId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/counts")
    public ResponseEntity<java.util.Map<Integer, Integer>> getEnrollmentCounts(@RequestParam List<Integer> courseIds) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentCounts(courseIds));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<java.util.Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(enrollmentService.getAdminStats());
    }
}
