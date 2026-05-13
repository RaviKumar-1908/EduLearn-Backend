package com.lms.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * NotificationServiceApplication – entry point for the LMS Notification microservice.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Registers with Eureka Server (port 8761) via {@code @EnableDiscoveryClient}</li>
 *   <li>Enables Redis-backed method caching via {@code @EnableCaching}</li>
 *   <li>Listens to RabbitMQ queues for enrollment / payment / quiz / certificate / course events</li>
 *   <li>Exposes REST endpoints on port 8086, accessible only through the API Gateway (port 8000)</li>
 * </ul>
 *
 * @author  LMS Team
 * @version 1.0.0
 */
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableAsync
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
