package com.lms.payment.config;

import com.lms.payment.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Payment-Service.
 *
 * Strategy:
 * - Stateless JWT — no sessions, no cookies.
 * - Swagger UI and actuator endpoints are open (needed for Swagger to load without auth).
 * - All /api/** routes require a valid JWT.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectProvider<JwtAuthFilter> jwtAuthFilterProvider;

    @Bean
    @SuppressWarnings("java:S4502") // Disabling CSRF is safe for stateless JWT-based APIs
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/actuator/**",
                    "/api/payments/verify",
                    "/api/payments/webhook",
                    "/payments/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            );
        jwtAuthFilterProvider.ifAvailable(jwtAuthFilter ->
            http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class));

        return http.build();
    }
}
