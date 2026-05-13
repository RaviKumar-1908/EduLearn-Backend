package com.lms.enrollment.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PROGRESS_QUEUE = "enrollment.progress.queue";
    public static final String CERTIFICATE_QUEUE = "enrollment.certificate.queue";
    public static final String EXCHANGE       = "lms.events.exchange";
    public static final String CERT_EXCHANGE   = "lms.certificate.exchange";
    public static final String ROUTING_KEY    = "progress.updated";
    public static final String CERT_ROUTING_KEY = "certificate.issued";
    public static final String COURSE_DELETED_QUEUE = "enrollment.course.deleted.queue";
    public static final String COURSE_DELETED_ROUTING_KEY = "notification.course.deleted";
    public static final String COURSE_PUBLISHED_QUEUE = "enrollment.course.published.queue";
    public static final String COURSE_PUBLISHED_ROUTING_KEY = "notification.course.published";
    public static final String LESSON_PUBLISHED_QUEUE = "enrollment.lesson.published.queue";
    public static final String LESSON_PUBLISHED_ROUTING_KEY = "notification.lesson.published";
    public static final String PAYMENT_SUCCESS_QUEUE = "enrollment.payment.success.queue";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "notification.payment.success";

    @Bean
    public org.springframework.amqp.core.Queue progressQueue() {
        return new org.springframework.amqp.core.Queue(PROGRESS_QUEUE);
    }

    @Bean
    public org.springframework.amqp.core.Queue certificateQueue() {
        return new org.springframework.amqp.core.Queue(CERTIFICATE_QUEUE);
    }

    @Bean
    public org.springframework.amqp.core.Queue courseDeletedQueue() {
        return new org.springframework.amqp.core.Queue(COURSE_DELETED_QUEUE);
    }

    @Bean
    public org.springframework.amqp.core.Queue coursePublishedQueue() {
        return new org.springframework.amqp.core.Queue(COURSE_PUBLISHED_QUEUE);
    }

    @Bean
    public org.springframework.amqp.core.Queue lessonPublishedQueue() {
        return new org.springframework.amqp.core.Queue(LESSON_PUBLISHED_QUEUE);
    }

    @Bean
    public org.springframework.amqp.core.Queue paymentSuccessQueue() {
        return new org.springframework.amqp.core.Queue(PAYMENT_SUCCESS_QUEUE);
    }

    @Bean
    public org.springframework.amqp.core.TopicExchange exchange() {
        return new org.springframework.amqp.core.TopicExchange(EXCHANGE);
    }

    @Bean
    public org.springframework.amqp.core.TopicExchange certExchange() {
        return new org.springframework.amqp.core.TopicExchange(CERT_EXCHANGE);
    }

    @Bean
    public org.springframework.amqp.core.Binding binding(org.springframework.amqp.core.Queue progressQueue, org.springframework.amqp.core.TopicExchange exchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(progressQueue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.core.Binding certBinding(org.springframework.amqp.core.Queue certificateQueue, org.springframework.amqp.core.TopicExchange certExchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(certificateQueue).to(certExchange).with(CERT_ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.core.Binding courseDeletedBinding(org.springframework.amqp.core.Queue courseDeletedQueue, org.springframework.amqp.core.TopicExchange exchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(courseDeletedQueue).to(exchange).with(COURSE_DELETED_ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.core.Binding coursePublishedBinding(org.springframework.amqp.core.Queue coursePublishedQueue, org.springframework.amqp.core.TopicExchange exchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(coursePublishedQueue).to(exchange).with(COURSE_PUBLISHED_ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.core.Binding lessonPublishedBinding(org.springframework.amqp.core.Queue lessonPublishedQueue, org.springframework.amqp.core.TopicExchange exchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(lessonPublishedQueue).to(exchange).with(LESSON_PUBLISHED_ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.core.Binding paymentSuccessBinding(org.springframework.amqp.core.Queue paymentSuccessQueue, org.springframework.amqp.core.TopicExchange exchange) {
        return org.springframework.amqp.core.BindingBuilder.bind(paymentSuccessQueue).to(exchange).with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

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
