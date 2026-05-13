package com.lms.notification.config;

import com.lms.notification.security.JwtAuthFilter;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationConfigTest {

    private RabbitMQConfig rabbitMQConfig;

    @BeforeEach
    void setUp() {
        rabbitMQConfig = new RabbitMQConfig();
        ReflectionTestUtils.setField(rabbitMQConfig, "exchange", "lms.events.exchange");
        ReflectionTestUtils.setField(rabbitMQConfig, "certificateExchange", "lms.certificate.exchange");
        ReflectionTestUtils.setField(rabbitMQConfig, "enrollmentQueue", "q.enrollment");
        ReflectionTestUtils.setField(rabbitMQConfig, "paymentQueue", "q.payment");
        ReflectionTestUtils.setField(rabbitMQConfig, "quizQueue", "q.quiz");
        ReflectionTestUtils.setField(rabbitMQConfig, "certificateQueue", "q.certificate");
        ReflectionTestUtils.setField(rabbitMQConfig, "courseQueue", "q.course");
        ReflectionTestUtils.setField(rabbitMQConfig, "authQueue", "q.auth");
        ReflectionTestUtils.setField(rabbitMQConfig, "discussionQueue", "q.discussion");
        ReflectionTestUtils.setField(rabbitMQConfig, "lessonQueue", "q.lesson");
        ReflectionTestUtils.setField(rabbitMQConfig, "progressQueue", "q.progress");
        ReflectionTestUtils.setField(rabbitMQConfig, "emailGeneralQueue", "q.email.general");
        ReflectionTestUtils.setField(rabbitMQConfig, "emailAuthQueue", "q.email.auth");
        ReflectionTestUtils.setField(rabbitMQConfig, "emailPurchaseQueue", "q.email.purchase");
        ReflectionTestUtils.setField(rabbitMQConfig, "emailInstructorQueue", "q.email.instructor");
    }

    @Test
    void mailConfig_buildsExpectedBeans() {
        MailConfig config = new MailConfig();

        ResourceBundleMessageSource messageSource = config.emailMessageSource();
        assertEquals("mail/messages", messageSource.getBasenameSet().iterator().next());
        assertNotNull(config.thymeleafTemplateResolver());
        assertNotNull(config.thymeleafTemplateEngine(config.thymeleafTemplateResolver()));
    }

    @Test
    void redisConfig_buildsCacheManager() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);

        CacheManager manager = config.cacheManager(factory);

        assertNotNull(manager);
    }

    @Test
    void rabbitConfig_buildsQueuesBindingsAndTemplate() {
        TopicExchange events = rabbitMQConfig.lmsEventsExchange();
        TopicExchange certificates = rabbitMQConfig.lmsCertificateExchange();
        Queue enrollment = rabbitMQConfig.enrollmentNotificationQueue();
        Queue payment = rabbitMQConfig.paymentNotificationQueue();
        Queue quiz = rabbitMQConfig.quizNotificationQueue();
        Queue certificate = rabbitMQConfig.certificateNotificationQueue();
        Queue course = rabbitMQConfig.courseNotificationQueue();
        Queue auth = rabbitMQConfig.authNotificationQueue();
        Queue discussion = rabbitMQConfig.discussionNotificationQueue();
        Queue lesson = rabbitMQConfig.lessonNotificationQueue();
        Queue progress = rabbitMQConfig.progressNotificationQueue();
        Queue emailGeneral = rabbitMQConfig.emailGeneralQueue();
        Queue emailAuth = rabbitMQConfig.emailAuthQueue();
        Queue emailPurchase = rabbitMQConfig.emailPurchaseQueue();
        Queue emailInstructor = rabbitMQConfig.emailInstructorQueue();

        assertEquals("lms.events.exchange", events.getName());
        assertEquals("lms.certificate.exchange", certificates.getName());
        assertEquals("q.enrollment", enrollment.getName());
        assertEquals("q.email.general", emailGeneral.getName());
        assertEquals(RabbitMQConfig.EMAIL_DLX, rabbitMQConfig.emailDeadLetterExchange().getName());
        assertEquals(RabbitMQConfig.EMAIL_DLQ, rabbitMQConfig.emailDeadLetterQueue().getName());

        assertNotNull(rabbitMQConfig.emailDlqBinding());
        assertBinding(rabbitMQConfig.enrollmentBinding(enrollment, events), "notification.enrollment.#");
        assertBinding(rabbitMQConfig.paymentBinding(payment, events), "notification.payment.#");
        assertBinding(rabbitMQConfig.quizBinding(quiz, events), "notification.quiz.#");
        assertBinding(rabbitMQConfig.certificateBinding(certificate, certificates), "notification.certificate.#");
        assertBinding(rabbitMQConfig.courseBinding(course, events), "notification.course.#");
        assertBinding(rabbitMQConfig.authBinding(auth, events), "notification.auth.#");
        assertBinding(rabbitMQConfig.discussionBinding(discussion, events), "notification.discussion.#");
        assertBinding(rabbitMQConfig.lessonBinding(lesson, events), "notification.lesson.#");
        assertBinding(rabbitMQConfig.progressBinding(progress, events), "progress.updated");
        assertBinding(rabbitMQConfig.emailGeneralBinding(emailGeneral, events), "notification.email.general");
        assertBinding(rabbitMQConfig.emailAuthBinding(emailAuth, events), "notification.email.auth");
        assertBinding(rabbitMQConfig.emailPurchaseBinding(emailPurchase, events), "notification.email.purchase");
        assertBinding(rabbitMQConfig.emailInstructorBinding(emailInstructor, events), "notification.email.instructor");

        assertNotNull(rabbitMQConfig.jsonMessageConverter());
        RabbitTemplate template = rabbitMQConfig.rabbitTemplate(mock(ConnectionFactory.class));
        assertNotNull(template.getMessageConverter());
    }

    @Test
    void swaggerConfig_buildsExpectedOpenApiMetadata() {
        SwaggerConfig config = new SwaggerConfig();

        OpenAPI openAPI = config.notificationServiceOpenAPI();

        assertTrue(openAPI.getInfo().getTitle().contains("Notification Service API"));
        assertEquals(2, openAPI.getServers().size());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("BearerAuth"));
    }

    @Test
    void webConfig_isLoadable() {
        WebConfig config = new WebConfig();
        assertNotNull(config);
    }

    @Test
    void securityConfig_isLoadable() {
        JwtAuthFilter filter = mock(JwtAuthFilter.class);
        SecurityConfig config = new SecurityConfig(filter);
        assertNotNull(config);
        // Validate CORS configuration presence
        assertNotNull(config.corsConfigurationSource());
    }

    @Test
    void webSocketConfig_registersBrokerAndEndpoints() {
        WebSocketConfig config = new WebSocketConfig();
        MessageBrokerRegistry registry = new MessageBrokerRegistry(mock(SubscribableChannel.class), mock(SubscribableChannel.class));
        config.configureMessageBroker(registry);

        StompEndpointRegistry endpointRegistry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJs = mock(SockJsServiceRegistration.class);
        when(endpointRegistry.addEndpoint("/ws-notifications")).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);
        when(registration.withSockJS()).thenReturn(sockJs);

        config.registerStompEndpoints(endpointRegistry);

        verify(endpointRegistry, times(2)).addEndpoint("/ws-notifications");
        verify(registration, times(2)).setAllowedOriginPatterns("*");
        verify(registration).withSockJS();
    }

    private void assertBinding(Binding binding, String routingKey) {
        assertNotNull(binding);
        assertEquals(routingKey, binding.getRoutingKey());
    }
}
