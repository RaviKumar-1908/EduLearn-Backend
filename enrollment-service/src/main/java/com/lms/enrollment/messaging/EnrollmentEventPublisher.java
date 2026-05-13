package com.lms.enrollment.messaging;

import com.lms.enrollment.entity.Enrollment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EnrollmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:lms.events.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.enrollment:notification.enrollment.success}")
    private String enrollmentKey;

    public void publishEnrollmentSuccess(Enrollment enrollment, String courseTitle) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", enrollment.getStudentId());
        event.put("title", "Enrollment Confirmed! 🎉");
        event.put("message", "You have successfully enrolled in " + courseTitle);
        event.put("relatedEntityId", enrollment.getCourseId());
        event.put("relatedEntityType", "COURSE");
        event.put("type", "ENROLLMENT");

        try {
            rabbitTemplate.convertAndSend(exchange, enrollmentKey, event);
            log.info("Published ENROLLMENT_SUCCESS event for studentId={} courseId={}", 
                enrollment.getStudentId(), enrollment.getCourseId());
        } catch (Exception e) {
            log.error("Failed to publish enrollment event: {}", e.getMessage());
        }
    }

    public void publishNewEnrollmentAlert(int instructorId, int studentId, int courseId, String studentName, String courseTitle) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", instructorId);
        event.put("title", "New Course Enrollment! 👤");
        event.put("message", studentName + " just joined your course " + courseTitle);
        event.put("relatedEntityId", courseId);
        event.put("relatedEntityType", "COURSE");
        event.put("type", "NEW_ENROLLMENT");

        try {
            rabbitTemplate.convertAndSend(exchange, "notification.enrollment.alert", event);
            log.info("Published NEW_ENROLLMENT alert for instructorId={} from studentId={}", instructorId, studentId);
        } catch (Exception e) {
            log.error("Failed to publish instructor enrollment alert: {}", e.getMessage());
        }
    }
    
    public void publishCourseUpdate(int userId, String title, String message, int courseId) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("title", title);
        event.put("message", message);
        event.put("relatedEntityId", courseId);
        event.put("relatedEntityType", "COURSE");
        event.put("type", "COURSE_UPDATE");

        try {
            rabbitTemplate.convertAndSend(exchange, "notification.enrollment.update", event);
        } catch (Exception e) {
            log.error("Failed to publish course update notification: {}", e.getMessage());
        }
    }
    
    public void publishCourseCompletion(Enrollment enrollment, String courseTitle) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", enrollment.getStudentId());
        event.put("title", "Course Completed! 🎓");
        event.put("message", "Congratulations! You have successfully completed " + courseTitle);
        event.put("relatedEntityId", enrollment.getCourseId());
        event.put("relatedEntityType", "COURSE");
        event.put("type", "COURSE_COMPLETED");

        try {
            rabbitTemplate.convertAndSend(exchange, "notification.enrollment.completion", event);
            log.info("Published COURSE_COMPLETED event for studentId={} courseId={}", 
                enrollment.getStudentId(), enrollment.getCourseId());
        } catch (Exception e) {
            log.error("Failed to publish course completion event: {}", e.getMessage());
        }
    }
}
