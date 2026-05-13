package com.lms.progress.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the Progress Service.
 *
 * Policy:
 * <ul>
 *   <li>Stateless – no HTTP session is created.</li>
 *   <li>CSRF disabled – REST APIs with JWT do not need CSRF protection.</li>
 *   <li>GET /certificates/verify is publicly accessible (third-party verification).</li>
 *   <li>Swagger UI and actuator are allowed without a token (dev-friendliness).</li>
 *   <li>All other requests require a valid JWT.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    @SuppressWarnings("java:S4502") // Disabling CSRF is safe for stateless JWT-based APIs
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Public: third-party certificate verification
                .requestMatchers("/certificates/verify", "/api/progress/certificates/verify", "/progress/certificates/verify").permitAll()
                // Public: Swagger UI and OpenAPI docs
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/api/progress/v3/api-docs/**",
                    "/api/progress/swagger-ui/**",
                    "/api/progress/swagger-ui.html"
                ).permitAll()
                // Public: Spring Boot Actuator health check
                .requestMatchers("/actuator/**").permitAll()
                // Everything else needs a valid JWT
                .anyRequest().authenticated()
            )

            // Plug in our JWT filter before the standard username/password filter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
