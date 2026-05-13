package com.lms.progress.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Servlet filter that runs once per request and validates the JWT
 * carried in the {@code Authorization: Bearer <token>} header.
 *
 * If the token is valid the filter sets a fully authenticated
 * {@link UsernamePasswordAuthenticationToken} in the SecurityContext,
 * allowing downstream filters and controllers to see the request as
 * authenticated.
 *
 * The public verify endpoint (/certificates/verify) is excluded
 * in {@link SecurityConfig} and does not reach this filter.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log     = LoggerFactory.getLogger(JwtFilter.class);
    private static final String BEARER  = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token present – let SecurityConfig decide whether to reject
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token    = authHeader.substring(BEARER.length());
        String username = null;

        try {
            username = jwtUtil.extractUsername(token);
        } catch (Exception ex) {
            log.warn("Failed to extract username from JWT: {}", ex.getMessage());
        }

        // Set authentication if token is valid and context is not already set
        if (username != null &&
            SecurityContextHolder.getContext().getAuthentication() == null &&
            jwtUtil.isTokenValid(token)) {

            Long userId = jwtUtil.extractUserId(token);
            
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userId != null ? userId : username,
                            null,
                            Collections.emptyList()   // roles can be extracted from claims if needed
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("JWT authenticated for userId: {}", userId);
        }

        filterChain.doFilter(request, response);
    }
}
