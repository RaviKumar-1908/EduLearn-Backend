package com.lms.notification.config;

import com.lms.notification.security.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * WebConfig – registers the {@link JwtAuthFilter} as a servlet filter.
 *
 * <p>The filter is set to highest precedence so JWT validation runs
 * before any other filter in the chain.
 *
 * @author LMS Team
 */
@Configuration
public class WebConfig {
    // JwtAuthFilter is now managed exclusively by SecurityConfig to avoid precedence issues.
}
