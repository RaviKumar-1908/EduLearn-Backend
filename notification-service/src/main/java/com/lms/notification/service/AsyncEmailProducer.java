package com.lms.notification.service;

import com.lms.notification.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:lms.events.exchange}")
    private String exchange;

    private static final String ROUTING_KEY = "notification.email.general";

    public void queueEmail(EmailRequest request) {
        if (request == null || request.getTo() == null || request.getTo().isBlank()) {
            log.warn("[AsyncEmailProducer] ⚠ Skipping email — null or missing recipient | request={}",
                    request);
            return;
        }

        log.info("[AsyncEmailProducer] ▶ Publishing email to exchange={} | routingKey={} | to={} | subject={}",
                exchange, ROUTING_KEY, request.getTo(), request.getSubject());

        try {
            rabbitTemplate.convertAndSend(exchange, ROUTING_KEY, request);
            log.info("[AsyncEmailProducer] ✔ Email queued successfully | to={} | subject={}",
                    request.getTo(), request.getSubject());
        } catch (AmqpException e) {
            // RabbitMQ connection/channel problem
            log.error(
                    "[AsyncEmailProducer] ✘ FAILED to queue email — RabbitMQ error | to={} | exchange={} | routingKey={} | error={}",
                    request.getTo(), exchange, ROUTING_KEY, e.getMessage(), e);
        } catch (Exception e) {
            // Serialization or unexpected error
            log.error("[AsyncEmailProducer] ✘ FAILED to queue email — unexpected error | to={} | error={}",
                    request.getTo(), e.getMessage(), e);
        }
    }
}