package com.lms.payment.messaging;

import com.lms.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes payment lifecycle events to RabbitMQ.
 *
 * Why RabbitMQ here?
 * The Notification-Service needs to know when payments succeed or are refunded
 * so it can send confirmation emails/SMS. Using a message queue means:
 *  - Payment-Service doesn't need to call Notification-Service directly (loose coupling).
 *  - If Notification-Service is down, messages queue up and get delivered later (resilience).
 */
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.payment-success}")
    private String paymentSuccessKey;

    @Value("${app.rabbitmq.routing-key.refund}")
    private String refundKey;

    /** Publishes a payment-success event after a successful transaction. */
    public void publishPaymentSuccess(Payment payment, Map<String, Object> metadata) {
        Map<String, Object> event = new HashMap<>();
        if (metadata != null) {
            event.putAll(metadata);
        }
        
        String courseTitle = metadata != null && metadata.get("courseTitle") != null 
            ? metadata.get("courseTitle").toString() 
            : "the course";

        event.put("userId", payment.getStudentId());
        event.put("title", "Payment Successful! ✅");
        event.put("message", String.format("Successfully paid %.2f %s for %s", 
            payment.getAmount(), payment.getCurrency(), courseTitle));
        event.put("relatedEntityId", payment.getCourseId());
        event.put("relatedEntityType", "COURSE");
        event.put("amount", payment.getAmount());
        event.put("paymentId", payment.getPaymentId());
        event.put("transactionId", payment.getTransactionId());

        try {
            log.info("[RabbitMQ] ▶ Publishing PAYMENT_SUCCESS event | userId={} | paymentId={}", payment.getStudentId(), payment.getPaymentId());
            rabbitTemplate.convertAndSend(exchange, paymentSuccessKey, event);
            log.info("[RabbitMQ] ✔ Event published successfully");
        } catch (Exception e) {
            log.error("[RabbitMQ] ✘ Failed to publish PAYMENT_SUCCESS event: {}", e.getMessage());
        }
    }

    /** Publishes a refund event so Notification-Service can alert the student. */
    public void publishRefund(Payment payment) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", payment.getStudentId());
        event.put("title", "Refund Processed ↩️");
        event.put("message", String.format("A refund of %.2f %s has been processed for your course", 
            payment.getAmount(), payment.getCurrency()));
        event.put("relatedEntityId", payment.getCourseId());
        event.put("relatedEntityType", "COURSE");

        try {
            log.info("[RabbitMQ] ▶ Publishing PAYMENT_REFUND event | userId={} | paymentId={}", payment.getStudentId(), payment.getPaymentId());
            rabbitTemplate.convertAndSend(exchange, refundKey, event);
            log.info("[RabbitMQ] ✔ Event published successfully");
        } catch (Exception e) {
            log.error("[RabbitMQ] ✘ Failed to publish PAYMENT_REFUND event: {}", e.getMessage());
        }
    }
}
