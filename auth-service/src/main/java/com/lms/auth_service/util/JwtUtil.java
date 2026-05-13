package com.lms.auth_service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Utility class to handle the creation and parsing of JSON Web Tokens (JWT).
 * A JWT consists of a Header, Payload, and Signature. We use an HMAC SHA-256 algorithm.
 */
@Component
public class JwtUtil {

    // The secret string stored safely in application.yml used to sign the token
    @Value("${jwt.secret:defaultSecretKeyLongEnoughForHmacSha256Algorithm}")
    private String secret;

    // Time to live for the JWT
    @Value("${jwt.expiration:86400000}") 
    private long tokenExpiry;

    /**
     * Converts our secret string into a cryptographic Key object used for the HMAC signature.
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Workflow: Generates a JWT string given an authenticated user's email and role.
     * 1. Sets the "Subject" (usually the unique identifier, like email).
     * 2. Sets Custom Claims (like "role").
     * 3. Sets IssuedAt (now) and Expiration time (now + expiry property).
     * 4. Signs the payload using the secret key to prevent client-side tampering.
     */
    public String generateToken(Integer userId, String email, String role, String fullName, String profilePicUrl, String gender) {
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .claim("fullName", fullName)
                .claim("profilePicUrl", profilePicUrl)
                .claim("gender", gender)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiry))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public boolean validateToken(String token, String userEmail) {
        final String email = extractEmail(token);
        return (email.equals(userEmail) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date());
    }
}
