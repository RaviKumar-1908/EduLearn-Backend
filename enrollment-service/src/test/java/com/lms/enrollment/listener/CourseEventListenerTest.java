package com.lms.enrollment.listener;

import com.lms.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseEventListenerTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseEventListener listener;

    @Test
    void onCourseDeleted_success() {
        Map<String, Object> event = Map.of("courseId", 101);

        listener.onCourseDeleted(event);

        verify(enrollmentRepository).deleteByCourseId(101);
    }

    @Test
    void onCourseDeleted_missingId_doesNothing() {
        listener.onCourseDeleted(Map.of());
        verify(enrollmentRepository, never()).deleteByCourseId(anyInt());
    }
}
