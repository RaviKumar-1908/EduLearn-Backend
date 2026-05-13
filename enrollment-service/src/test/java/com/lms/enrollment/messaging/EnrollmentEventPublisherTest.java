package com.lms.enrollment.messaging;

import com.lms.enrollment.entity.Enrollment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EnrollmentEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "exchange", "test.exchange");
        ReflectionTestUtils.setField(publisher, "enrollmentKey", "test.key");
    }

    @Test
    void publishEnrollmentSuccess_success() {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(10);
        enrollment.setCourseId(101);

        publisher.publishEnrollmentSuccess(enrollment, "Java 101");

        verify(rabbitTemplate).convertAndSend(eq("test.exchange"), eq("test.key"), anyMap());
    }

    @Test
    void publishEnrollmentSuccess_exception_logsError() {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(10);
        enrollment.setCourseId(101);

        doThrow(new RuntimeException("Rabbit error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        publisher.publishEnrollmentSuccess(enrollment, "Java 101");

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyMap());
    }
}
