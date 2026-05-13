package com.lms.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/ws-notifications");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug("[JwtAuthFilter] Processing request: {} {}", method, path);

        // ── FIX 1: Allow OPTIONS requests (CORS preflight) ──
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.debug("[JwtAuthFilter] Allowing OPTIONS request for: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        // ── FIX 2: Do NOT return 401 immediately if header is missing ──
        // Let Spring Security handle the authorization logic based on SecurityConfig.
        // If the path is permitAll(), it will proceed. If it's authenticated(), 
        // Spring Security will reject it later with a proper entry point.
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("[JwtAuthFilter] No valid Bearer token found for: {}. Proceeding to SecurityFilterChain.", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtUtil.isTokenValid(token)) {
            log.warn("[JwtAuthFilter] Invalid or expired JWT for: {}", path);
            // Even if invalid, we can proceed and let SecurityContext remain empty, 
            // causing a 403/401 later if authentication is required.
            // Or we can return 401 if we want to be strict for all requests with tokens.
            // For now, let's keep it strict if a token IS provided.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"statusCode\":401,\"message\":\"JWT token is invalid or expired\"}");
            return;
        }

        try {
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);
            Integer userId = jwtUtil.extractUserId(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities = role == null || role.isBlank()
                        ? List.of()
                        : List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );
                
                Map<String, Object> authDetails = new HashMap<>();
                authDetails.put("userId", userId);
                authDetails.put("username", username);
                authDetails.put("role", role);
                // Building standard details as well
                authDetails.put("remoteAddress", request.getRemoteAddr());
                
                authentication.setDetails(authDetails);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("[JwtAuthFilter] Successfully authenticated user='{}' (userId={})", username, userId);
            }
        } catch (Exception e) {
            log.error("[JwtAuthFilter] Error processing JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
