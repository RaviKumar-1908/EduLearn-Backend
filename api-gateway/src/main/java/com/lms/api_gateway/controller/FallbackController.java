package com.lms.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @GetMapping("/fallback/courseService")
    public Mono<String> courseServiceFallback() {
        return Mono.just("Course Service is taking too long to respond or is down. Please try again later.");
    }

    @GetMapping("/fallback/authService")
    public Mono<String> authServiceFallback() {
        return Mono.just("Authentication Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/fallback/general")
    public Mono<String> generalFallback() {
        return Mono.just("The requested service is currently unavailable. We are working to fix it!");
    }
}
