package com.lms.enrollment.listener;

import com.lms.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateEventListener {

    private final EnrollmentRepository enrollmentRepository;

    @RabbitListener(queues = "enrollment.certificate.queue")
    @Transactional
    public void onCertificateIssued(Map<String, Object> event) {
        Integer studentId = (Integer) event.get("studentId");
        Integer courseId = (Integer) event.get("courseId");
        
        if (studentId != null && courseId != null) {
            log.info("Received CERTIFICATE_ISSUED event for studentId: {}, courseId: {}", studentId, courseId);
            
            enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .ifPresentOrElse(enrollment -> {
                    enrollment.setCertificateIssued(true);
                    enrollmentRepository.save(enrollment);
                    log.info("Successfully updated enrollment to certificateIssued=true for enrollmentId: {}", enrollment.getEnrollmentId());
                }, () -> {
                    log.warn("No enrollment found for studentId: {} and courseId: {} to mark certificate as issued", studentId, courseId);
                });
        }
    }
}
