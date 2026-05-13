package com.lms.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Dead Letter Constants ─────────────────────────────────────────────────
    public static final String EMAIL_DLQ = "email.dlq";
    public static final String EMAIL_DLX = "email.dlx";

    // ── Injected Queue / Exchange Names ───────────────────────────────────────
    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.exchange.certificate:lms.certificate.exchange}")
    private String certificateExchange;

    @Value("${app.rabbitmq.queue.enrollment}")
    private String enrollmentQueue;

    @Value("${app.rabbitmq.queue.payment}")
    private String paymentQueue;

    @Value("${app.rabbitmq.queue.quiz}")
    private String quizQueue;

    @Value("${app.rabbitmq.queue.certificate}")
    private String certificateQueue;

    @Value("${app.rabbitmq.queue.course}")
    private String courseQueue;

    @Value("${app.rabbitmq.queue.auth}")
    private String authQueue;

    @Value("${app.rabbitmq.queue.discussion}")
    private String discussionQueue;

    @Value("${app.rabbitmq.queue.lesson}")
    private String lessonQueue;

    @Value("${app.rabbitmq.queue.progress}")
    private String progressQueue;

    @Value("${app.rabbitmq.queue.email.general}")
    private String emailGeneralQueue;

    @Value("${app.rabbitmq.queue.email.auth}")
    private String emailAuthQueue;

    @Value("${app.rabbitmq.queue.email.purchase}")
    private String emailPurchaseQueue;

    @Value("${app.rabbitmq.queue.email.instructor}")
    private String emailInstructorQueue;

    // ── Dead Letter Exchange & Queue ──────────────────────────────────────────

    @Bean
    public DirectExchange emailDeadLetterExchange() {
        return new DirectExchange(EMAIL_DLX);
    }

    /**
     * FIX 1: Made durable so it survives broker restart.
     * Previously: new Queue(EMAIL_DLQ) — non-durable by default.
     */
    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder
                .bind(emailDeadLetterQueue())
                .to(emailDeadLetterExchange())
                .with("email.failed");
    }

    // ── Exchanges ─────────────────────────────────────────────────────────────

    /**
     * Primary topic exchange — used by all services except certificate events.
     */
    @Bean
    public TopicExchange lmsEventsExchange() {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    /**
     * FIX 2: Separate exchange for certificate events.
     * Previously missing — certificate queue was bound to lms.events.exchange
     * but the Certificate Service publishes to lms.certificate.exchange,
     * so those events were never received.
     */
    @Bean
    public TopicExchange lmsCertificateExchange() {
        return ExchangeBuilder.topicExchange(certificateExchange).durable(true).build();
    }

    // ── Queues ────────────────────────────────────────────────────────────────

    @Bean
    public Queue enrollmentNotificationQueue() {
        return QueueBuilder.durable(enrollmentQueue).build();
    }

    @Bean
    public Queue paymentNotificationQueue() {
        return QueueBuilder.durable(paymentQueue).build();
    }

    @Bean
    public Queue quizNotificationQueue() {
        return QueueBuilder.durable(quizQueue).build();
    }

    @Bean
    public Queue certificateNotificationQueue() {
        return QueueBuilder.durable(certificateQueue).build();
    }

    @Bean
    public Queue courseNotificationQueue() {
        return QueueBuilder.durable(courseQueue).build();
    }

    @Bean
    public Queue authNotificationQueue() {
        return QueueBuilder.durable(authQueue).build();
    }

    @Bean
    public Queue discussionNotificationQueue() {
        return QueueBuilder.durable(discussionQueue).build();
    }

    @Bean
    public Queue lessonNotificationQueue() {
        return QueueBuilder.durable(lessonQueue).build();
    }

    @Bean
    public Queue progressNotificationQueue() {
        return QueueBuilder.durable(progressQueue).build();
    }

    @Bean
    public Queue emailGeneralQueue() {
        return QueueBuilder.durable(emailGeneralQueue)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", "email.failed")
                .build();
    }

    @Bean
    public Queue emailAuthQueue() {
        return QueueBuilder.durable(emailAuthQueue)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", "email.failed")
                .build();
    }

    @Bean
    public Queue emailPurchaseQueue() {
        return QueueBuilder.durable(emailPurchaseQueue)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", "email.failed")
                .build();
    }

    @Bean
    public Queue emailInstructorQueue() {
        return QueueBuilder.durable(emailInstructorQueue)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", "email.failed")
                .build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @Bean
    public Binding enrollmentBinding(Queue enrollmentNotificationQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(enrollmentNotificationQueue)
                .to(lmsEventsExchange).with("notification.enrollment.#");
    }

    @Bean
    public Binding paymentBinding(Queue paymentNotificationQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(paymentNotificationQueue)
                .to(lmsEventsExchange).with("notification.payment.#");
    }

    @Bean
    public Binding quizBinding(Queue quizNotificationQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(quizNotificationQueue)
                .to(lmsEventsExchange).with("notification.quiz.#");
    }

    /**
     * FIX 2 (continued): Certificate queue now binds to lmsCertificateExchange
     * instead of lmsEventsExchange.
     */
    @Bean
    public Binding certificateBinding(Queue certificateNotificationQueue,
            TopicExchange lmsCertificateExchange) {
        return BindingBuilder.bind(certificateNotificationQueue)
                .to(lmsCertificateExchange).with("notification.certificate.#");
    }

    @Bean
    public Binding courseBinding(Queue courseNotificationQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(courseNotificationQueue)
                .to(lmsEventsExchange).with("notification.course.#");
    }

    @Bean
    public Binding authBinding(Queue authNotificationQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(authNotificationQueue)
                .to(lmsEventsExchange).with("notification.auth.#");
    }

    @Bean
    public Binding discussionBinding(Queue discussionNotificationQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(discussionNotificationQueue)
                .to(lmsEventsExchange).with("notification.discussion.#");
    }

    @Bean
    public Binding lessonBinding(Queue lessonNotificationQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(lessonNotificationQueue)
                .to(lmsEventsExchange).with("notification.lesson.#");
    }

    @Bean
    public Binding progressBinding(Queue progressNotificationQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(progressNotificationQueue)
                .to(lmsEventsExchange).with("progress.updated");
    }

    @Bean
    public Binding emailGeneralBinding(Queue emailGeneralQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(emailGeneralQueue)
                .to(lmsEventsExchange).with("notification.email.general");
    }

    @Bean
    public Binding emailAuthBinding(Queue emailAuthQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(emailAuthQueue)
                .to(lmsEventsExchange).with("notification.email.auth");
    }

    @Bean
    public Binding emailPurchaseBinding(Queue emailPurchaseQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(emailPurchaseQueue)
                .to(lmsEventsExchange).with("notification.email.purchase");
    }

    @Bean
    public Binding emailInstructorBinding(Queue emailInstructorQueue,
            TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(emailInstructorQueue)
                .to(lmsEventsExchange).with("notification.email.instructor");
    }

    // ── Message Converter ─────────────────────────────────────────────────────

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