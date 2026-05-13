package com.lms.progress.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ topology consumed by the Progress Service.
 *
 * Topology:
 *   Exchange : lms.certificate.exchange  (Topic)
 *   Queue    : lms.certificate.queue
 *   Binding  : certificate.issued → lms.certificate.queue
 *
 * The Notification Service binds its own queue to the same exchange
 * to receive {@code CertificateIssuedEvent} messages.
 */
@Configuration
public class RabbitMQConfig {

    public static final String CERTIFICATE_EXCHANGE    = "lms.events.exchange";
    public static final String CERTIFICATE_QUEUE       = "lms.certificate.queue";
    public static final String CERTIFICATE_ROUTING_KEY = "notification.certificate.issued";

    /* ------------------------------------------------------------------ */
    /* Exchange                                                             */
    /* ------------------------------------------------------------------ */

    @Bean
    public TopicExchange certificateExchange() {
        return new TopicExchange(CERTIFICATE_EXCHANGE);
    }

    /* ------------------------------------------------------------------ */
    /* Queue                                                                */
    /* ------------------------------------------------------------------ */

    @Bean
    public Queue certificateQueue() {
        return QueueBuilder.durable(CERTIFICATE_QUEUE).build();
    }

    /* ------------------------------------------------------------------ */
    /* Binding                                                              */
    /* ------------------------------------------------------------------ */

    @Bean
    public Binding certificateBinding(Queue certificateQueue, TopicExchange certificateExchange) {
        return BindingBuilder
                .bind(certificateQueue)
                .to(certificateExchange)
                .with(CERTIFICATE_ROUTING_KEY);
    }

    /* ------------------------------------------------------------------ */
    /* Message converter – use JSON so messages are human-readable         */
    /* ------------------------------------------------------------------ */

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
