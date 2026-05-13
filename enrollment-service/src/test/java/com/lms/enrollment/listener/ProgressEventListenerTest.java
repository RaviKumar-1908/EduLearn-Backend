package com.lms.enrollment.listener;

import com.lms.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressEventListenerTest {

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private ProgressEventListener listener;

    @Test
    void onProgressUpdated_success() {
        Map<String, Object> event = Map.of(
                "studentId", 1,
                "courseId", 101,
                "progressPercent", 75
        );

        listener.onProgressUpdated(event);

        verify(enrollmentService).updateProgress(1, 101, 75);
    }

    @Test
    void onProgressUpdated_missingData_doesNothing() {
        listener.onProgressUpdated(Map.of("studentId", 1));
        verify(enrollmentService, never()).updateProgress(anyInt(), anyInt(), anyInt());
    }

    @Test
    void onProgressUpdated_exception_logsError() {
        Map<String, Object> event = Map.of(
                "studentId", 1,
                "courseId", 101,
                "progressPercent", 75
        );

        doThrow(new RuntimeException("Update failed")).when(enrollmentService)
                .updateProgress(1, 101, 75);

        listener.onProgressUpdated(event);

        verify(enrollmentService).updateProgress(1, 101, 75);
    }
}
