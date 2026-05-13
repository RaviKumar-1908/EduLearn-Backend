package com.lms.enrollment.listener;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateEventListenerTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CertificateEventListener listener;

    @Test
    void onCertificateIssued_success() {
        Map<String, Object> event = Map.of("studentId", 1, "courseId", 101);
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(500);

        when(enrollmentRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(enrollment));

        listener.onCertificateIssued(event);

        assertTrue(enrollment.isCertificateIssued());
        verify(enrollmentRepository).save(enrollment);
    }

    private void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("Expected true");
    }

    @Test
    void onCertificateIssued_notFound_logsWarning() {
        Map<String, Object> event = Map.of("studentId", 1, "courseId", 101);
        when(enrollmentRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.empty());

        listener.onCertificateIssued(event);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void onCertificateIssued_missingIds_doesNothing() {
        listener.onCertificateIssued(Map.of());
        verify(enrollmentRepository, never()).findByStudentIdAndCourseId(anyInt(), anyInt());
    }
}
