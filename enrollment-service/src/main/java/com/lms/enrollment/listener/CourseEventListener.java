package com.lms.enrollment.listener;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.messaging.EnrollmentEventPublisher;
import com.lms.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEventListener {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentEventPublisher eventPublisher;

    @RabbitListener(queues = "enrollment.course.deleted.queue")
    @Transactional
    public void onCourseDeleted(Map<String, Object> event) {
        Integer courseId = (Integer) event.get("courseId");
        if (courseId != null) {
            log.info("Received COURSE_DELETED event for courseId: {}. Cleaning up enrollments...", courseId);
            enrollmentRepository.deleteByCourseId(courseId);
            log.info("Successfully deleted all enrollments for courseId: {}", courseId);
        }
    }

    @RabbitListener(queues = "enrollment.course.published.queue")
    public void onCoursePublished(Map<String, Object> event) {
        notifyEnrolledStudents(event, "COURSE_PUBLISHED");
    }

    @RabbitListener(queues = "enrollment.lesson.published.queue")
    public void onLessonPublished(Map<String, Object> event) {
        notifyEnrolledStudents(event, "LESSON_PUBLISHED");
    }

    private void notifyEnrolledStudents(Map<String, Object> event, String type) {
        Integer courseId = (Integer) event.get("courseId");
        String title = (String) event.get("title");
        String message = (String) event.get("message");
        
        if (courseId != null) {
            log.info("Received {} event for courseId: {}. Notifying enrolled students...", type, courseId);
            List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
            enrollments.stream()
                .filter(e -> "Active".equals(e.getStatus()) || "Completed".equals(e.getStatus()))
                .forEach(e -> eventPublisher.publishCourseUpdate(e.getStudentId(), title, message, courseId));
            log.info("Notification dispatch complete for courseId: {}", courseId);
        }
    }
}
