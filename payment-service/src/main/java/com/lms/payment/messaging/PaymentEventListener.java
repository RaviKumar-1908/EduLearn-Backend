package com.lms.payment.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.lms.payment.config.RabbitMQConfig;

import java.util.Map;

/**
 * Listens for inbound events from other LMS microservices.
 *
 * <p>
 * Example use-case: when the Enrollment Service sends an event confirming
 * a free enrollment, this listener could trigger a FREE plan subscription.
 * Extend this class with actual business logic as services are integrated.
 */
@Component
@Slf4j
public class PaymentEventListener {

    /**
     * Receives events published on the payment.queue by this service or others.
     * Currently acts as a dead-letter / audit logger.
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handleInboundPaymentEvent(Map<String, Object> event) {
        log.info("Inbound payment event received | type={} paymentId={}",
                event.get("eventType"), event.get("paymentId"));
    }
}
