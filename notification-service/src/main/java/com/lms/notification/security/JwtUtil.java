package com.lms.notification.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JwtUtil – stateless helper for JWT token parsing and validation.
 *
 * <p>The Notification-Service is a consumer-only; it <em>validates</em> tokens
 * produced by the Auth-Service and extracts claims. It does not issue tokens.
 *
 * <p>The shared HMAC-SHA256 secret must match the value used in the Auth-Service.
 *
 * @author LMS Team
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Builds the signing key from the configured secret.
     *
     * @return HMAC-SHA key
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Parses the JWT and returns all claims.
     *
     * @param token raw Bearer token (without "Bearer " prefix)
     * @return {@link Claims} map
     * @throws JwtException if the token is malformed, expired, or uses the wrong key
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the {@code sub} (subject / username) claim from the token.
     *
     * @param token raw JWT
     * @return username string
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return role == null ? null : role.toString();
    }

    public Integer extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        if (userId == null) {
            return null;
        }
        if (userId instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(userId.toString());
        } catch (NumberFormatException ex) {
            log.warn("Unable to parse userId claim from JWT: {}", userId);
            return null;
        }
    }

    /**
     * Validates a JWT token.
     *
     * <p>Checks:
     * <ol>
     *   <li>Signature integrity (correct secret)</li>
     *   <li>Expiry date</li>
     *   <li>Malformed structure</li>
     * </ol>
     *
     * @param token raw JWT
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            boolean expired = claims.getExpiration().before(new Date());
            if (expired) {
                log.warn("JWT token is expired");
            }
            return !expired;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT unsupported: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT malformed: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }
}
