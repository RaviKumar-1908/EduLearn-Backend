package com.lms.progress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;

/**
 * Entry point for the LMS Progress and Certificate Microservice.
 * Registers with Eureka and enables Spring Cache support.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
public class ProgressServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProgressServiceApplication.class, args);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
