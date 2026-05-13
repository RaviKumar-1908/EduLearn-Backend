package com.lms.payment.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
// import java.util.Date;

/**
 * Utility for validating JWT tokens issued by the Auth-Service.
 * The Payment-Service only validates tokens — it never issues them.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Extracts the username (subject) from a valid JWT. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns true if the token is syntactically valid and not expired. */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException | MalformedJwtException ex) {
            log.warn("JWT malformed/unsupported: {}", ex.getMessage());
        } catch (SignatureException ex) {
            log.warn("JWT signature mismatch: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims empty: {}", ex.getMessage());
        }
        return false;
    }

    /** Extracts the role claim from the JWT. */
    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role == null ? null : role.toString();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
