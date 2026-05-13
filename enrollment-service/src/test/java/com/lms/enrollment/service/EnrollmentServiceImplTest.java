package com.lms.enrollment.service;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.messaging.EnrollmentEventPublisher;
import com.lms.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentEventPublisher eventPublisher;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private Enrollment mockEnrollment;

    @BeforeEach
    void setUp() {
        mockEnrollment = new Enrollment(1, 101);
        mockEnrollment.setEnrollmentId(1001);
        mockEnrollment.setStatus("Active");
    }

    @Test
    void enroll_success() {
        when(enrollmentRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.empty());
        when(enrollmentRepository.existsActiveByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        Enrollment result = enrollmentService.enroll(1, 101, 49.99);

        assertNotNull(result);
        assertEquals("Active", result.getStatus());
        verify(eventPublisher, times(1)).publishEnrollmentSuccess(any(), any());
    }

    @Test
    void enroll_alreadyEnrolled_throwsException() {
        when(enrollmentRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(mockEnrollment));

        assertThrows(IllegalStateException.class, () -> enrollmentService.enroll(1, 101));
    }

    @Test
    void updateProgress_success() {
        when(enrollmentRepository.findActiveByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(mockEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        enrollmentService.updateProgress(1, 101, 50);

        assertEquals(50, mockEnrollment.getProgressPercent());
        verify(enrollmentRepository, times(1)).save(mockEnrollment);
    }

    @Test
    void markComplete_success() {
        when(enrollmentRepository.findActiveByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(mockEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        enrollmentService.markComplete(1, 101);

        assertEquals("Completed", mockEnrollment.getStatus());
        assertEquals(100, mockEnrollment.getProgressPercent());
    }

    @Test
    void unenroll_success() {
        when(enrollmentRepository.findActiveByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(mockEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        enrollmentService.unenroll(1, 101);

        assertEquals("Cancelled", mockEnrollment.getStatus());
    }

    // ==================== NEW TEST CASES FOR 80%+ COVERAGE ====================

    @Test
    void enroll_reEnrollAfterCancellation_success() {
        mockEnrollment.setStatus("Cancelled");
        when(enrollmentRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(mockEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        Enrollment result = enrollmentService.enroll(1, 101, 0.0);
        assertEquals("Active", result.getStatus());
        verify(enrollmentRepository).save(mockEnrollment);
    }

    @Test
    void updateProgress_invalidPercent_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> enrollmentService.updateProgress(1, 101, 150));
        assertThrows(IllegalArgumentException.class, () -> enrollmentService.updateProgress(1, 101, -1));
    }

    @Test
    void updateProgress_markCompleteAt100_success() {
        when(enrollmentRepository.findActiveByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(mockEnrollment));
        when(enrollmentRepository.save(any())).thenReturn(mockEnrollment);

        enrollmentService.updateProgress(1, 101, 100);
        assertEquals("Completed", mockEnrollment.getStatus());
        assertNotNull(mockEnrollment.getCompletedAt());
    }

    @Test
    void updateProgress_notFound_throwsException() {
        when(enrollmentRepository.findActiveByStudentIdAndCourseId(1, 101)).thenReturn(Optional.empty());
        when(enrollmentRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> enrollmentService.updateProgress(1, 101, 50));
    }

    @Test
    void issueCertificate_success() {
        mockEnrollment.setStatus("Completed");
        mockEnrollment.setCertificateIssued(false);
        when(enrollmentRepository.findActiveByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(mockEnrollment));

        enrollmentService.issueCertificate(1, 101);
        assertTrue(mockEnrollment.isCertificateIssued());
        verify(enrollmentRepository).save(mockEnrollment);
    }

    @Test
    void issueCertificate_notComplete_throwsException() {
        mockEnrollment.setStatus("Active");
        when(enrollmentRepository.findActiveByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(mockEnrollment));

        assertThrows(IllegalStateException.class, () -> enrollmentService.issueCertificate(1, 101));
    }

    @Test
    void issueCertificate_alreadyIssued_throwsException() {
        mockEnrollment.setStatus("Completed");
        mockEnrollment.setCertificateIssued(true);
        when(enrollmentRepository.findActiveByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(mockEnrollment));

        assertThrows(IllegalStateException.class, () -> enrollmentService.issueCertificate(1, 101));
    }

    @Test
    void getAdminStats_success() {
        when(enrollmentRepository.count()).thenReturn(100L);
        when(enrollmentRepository.countByStatus("Active")).thenReturn(80);
        when(enrollmentRepository.countByStatus("Completed")).thenReturn(20);

        java.util.Map<String, Object> stats = enrollmentService.getAdminStats();
        assertEquals(100L, stats.get("totalEnrollments"));
        assertEquals(80, stats.get("activeEnrollments"));
    }

    @Test
    void getEnrollmentCounts_success() {
        when(enrollmentRepository.countActiveByCourseId(101)).thenReturn(10);
        when(enrollmentRepository.countActiveByCourseId(102)).thenReturn(0);
        when(enrollmentRepository.countByCourseId(102)).thenReturn(5);

        java.util.Map<Integer, Integer> result = enrollmentService.getEnrollmentCounts(List.of(101, 102));
        assertEquals(10, result.get(101));
        assertEquals(5, result.get(102));
    }
}
