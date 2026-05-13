package com.lms.auth_service.config;

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

    @Value("${app.rabbitmq.exchange:lms.events.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queue.auth:auth.notification.queue}")
    private String authQueue;

    @Bean
    public TopicExchange lmsEventsExchange() {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    @Bean
    public Queue authNotificationQueue() {
        return QueueBuilder.durable(authQueue).build();
    }

    @Bean
    public Binding authBinding(Queue authNotificationQueue, TopicExchange lmsEventsExchange) {
        return BindingBuilder.bind(authNotificationQueue)
                .to(lmsEventsExchange)
                .with("notification.auth.#");
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
