package com.lms.course_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
    @SuppressWarnings("java:S4502") // Disabling CSRF is safe for stateless JWT-based APIs
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/**").permitAll()
                        .requestMatchers("/api/course/instructor/**", "/api/courses/instructor/**", "/courses/instructor/**").permitAll() 
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                        .requestMatchers(HttpMethod.GET, "/api/course/**", "/api/courses/**", "/courses/**").permitAll()
                        .requestMatchers("/api/course/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/course/**", "/api/courses/**", "/courses/**").hasAnyAuthority("ROLE_INSTRUCTOR", "ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
