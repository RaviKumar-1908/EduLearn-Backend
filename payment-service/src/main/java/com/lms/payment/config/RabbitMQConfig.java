package com.lms.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ topology for the Payment-Service.
 *
 * Topology:
 *   Exchange: payment.exchange  (TOPIC type — allows wildcard routing)
 *   Queue:    payment.success.queue  ← routing key: payment.success
 *   Queue:    payment.refund.queue   ← routing key: payment.refund
 *
 * The Notification-Service binds its own consumer queues to this same exchange.
 */
@Configuration
public class RabbitMQConfig {

    public static final String SUCCESS_QUEUE = "payment.success.queue";
    public static final String REFUND_QUEUE  = "payment.refund.queue";
    public static final String PAYMENT_QUEUE = SUCCESS_QUEUE; // Alias for internal listening/audit

	@Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.payment-success}")
    private String paymentSuccessKey;

    @Value("${app.rabbitmq.routing-key.refund}")
    private String refundKey;

    // ── Exchange ──────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(exchange);
    }

    // ── Queues ────────────────────────────────────────────────────────────────

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(SUCCESS_QUEUE).build();
    }

    @Bean
    public Queue paymentRefundQueue() {
        return QueueBuilder.durable(REFUND_QUEUE).build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @Bean
    public Binding bindSuccess(Queue paymentSuccessQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentSuccessQueue).to(paymentExchange).with(paymentSuccessKey);
    }

    @Bean
    public Binding bindRefund(Queue paymentRefundQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentRefundQueue).to(paymentExchange).with(refundKey);
    }

    // ── Template with JSON serialiser ─────────────────────────────────────────

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
