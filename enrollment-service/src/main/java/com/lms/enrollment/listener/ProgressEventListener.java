package com.lms.enrollment.listener;

import com.lms.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProgressEventListener {

    private final EnrollmentService enrollmentService;

    @RabbitListener(queues = "enrollment.progress.queue")
    public void onProgressUpdated(Map<String, Object> event) {
        Integer studentId = (Integer) event.get("studentId");
        Integer courseId = (Integer) event.get("courseId");
        Integer progressPercent = (Integer) event.get("progressPercent");

        if (studentId != null && courseId != null && progressPercent != null) {
            log.info("Received PROGRESS_UPDATED event | studentId={}, courseId={}, progress={}%", 
                    studentId, courseId, progressPercent);
            try {
                enrollmentService.updateProgress(studentId, courseId, progressPercent);
                log.info("Updated enrollment progress for studentId={} courseId={}", studentId, courseId);
            } catch (Exception e) {
                log.error("Failed to update enrollment progress from event", e);
            }
        }
    }
}
