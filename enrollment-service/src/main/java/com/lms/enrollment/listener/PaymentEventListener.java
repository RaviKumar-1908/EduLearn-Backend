package com.lms.enrollment.listener;

import com.lms.enrollment.config.RabbitMQConfig;
import com.lms.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens for payment success events from the payment-service.
 * This ensures that even if the frontend fails to call the enrollment API,
 * the student is automatically enrolled once payment is verified.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);
    private final EnrollmentService enrollmentService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(Map<String, Object> event) {
        log.info("[RabbitMQ] 📩 Received PAYMENT_SUCCESS event: {}", event);

        try {
            // Extract IDs from event payload
            // Expecting keys: userId (studentId), relatedEntityId (courseId), amount
            Object userIdObj = event.get("userId");
            Object courseIdObj = event.get("relatedEntityId");
            Object amountObj = event.get("amount");

            if (userIdObj == null || courseIdObj == null) {
                log.warn("[RabbitMQ] ⚠ Incomplete payment event data. userId or courseId is null.");
                return;
            }

            int studentId = Integer.parseInt(userIdObj.toString());
            int courseId = Integer.parseInt(courseIdObj.toString());
            double price = amountObj != null ? Double.parseDouble(amountObj.toString()) : 0.0;

            log.info("[RabbitMQ] 🔄 Auto-enrolling student {} in course {}", studentId, courseId);
            
            // Check if already enrolled to maintain idempotency
            if (!enrollmentService.isEnrolled(studentId, courseId)) {
                enrollmentService.enroll(studentId, courseId, price);
                log.info("[RabbitMQ] ✅ Auto-enrollment successful for student {} and course {}", studentId, courseId);
            } else {
                log.info("[RabbitMQ] ℹ Student {} is already enrolled in course {}. Skipping auto-enrollment.", studentId, courseId);
            }

        } catch (Exception e) {
            log.error("[RabbitMQ] ❌ Failed to process payment success event: {}", e.getMessage(), e);
        }
    }
}
