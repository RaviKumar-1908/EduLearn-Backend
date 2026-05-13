package com.lms.api_gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallbackController.class)
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void courseServiceFallback() {
        webTestClient.get().uri("/fallback/courseService")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Course Service is taking too long to respond or is down. Please try again later.");
    }

    @Test
    void authServiceFallback() {
        webTestClient.get().uri("/fallback/authService")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Authentication Service is currently unavailable. Please try again later.");
    }

    @Test
    void generalFallback() {
        webTestClient.get().uri("/fallback/general")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("The requested service is currently unavailable. We are working to fix it!");
    }
}
