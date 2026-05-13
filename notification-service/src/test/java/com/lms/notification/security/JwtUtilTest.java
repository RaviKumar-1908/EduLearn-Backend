package com.lms.notification.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
    }

    @Test
    void extractClaimsAndValidateToken_success() {
        String token = createToken(new Date(System.currentTimeMillis() + 60_000), 10);

        assertEquals("student@example.com", jwtUtil.extractUsername(token));
        assertEquals("STUDENT", jwtUtil.extractRole(token));
        assertEquals(10, jwtUtil.extractUserId(token));
        assertTrue(jwtUtil.isTokenValid(token));
        assertNotNull(jwtUtil.extractAllClaims(token));
    }

    @Test
    void extractUserId_supportsStringValues() {
        String token = Jwts.builder()
                .setSubject("student@example.com")
                .claim("role", "STUDENT")
                .claim("userId", "42")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertEquals(42, jwtUtil.extractUserId(token));
    }

    @Test
    void extractUserId_returnsNullForMissingOrInvalidValue() {
        String missingUserIdToken = Jwts.builder()
                .setSubject("student@example.com")
                .claim("role", "STUDENT")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        String invalidUserIdToken = Jwts.builder()
                .setSubject("student@example.com")
                .claim("role", "STUDENT")
                .claim("userId", "abc")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertNull(jwtUtil.extractUserId(missingUserIdToken));
        assertNull(jwtUtil.extractUserId(invalidUserIdToken));
    }

    @Test
    void isTokenValid_returnsFalseForExpiredMalformedAndWrongSecretTokens() {
        String expiredToken = createToken(new Date(System.currentTimeMillis() - 60_000), 10);
        String malformedToken = "not-a-jwt";
        String wrongSecretToken = Jwts.builder()
                .setSubject("student@example.com")
                .claim("role", "STUDENT")
                .claim("userId", 10)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("abcdefghijklmnopqrstuvwxyz123456".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(expiredToken));
        assertFalse(jwtUtil.isTokenValid(malformedToken));
        assertFalse(jwtUtil.isTokenValid(wrongSecretToken));
    }

    private String createToken(Date expiration, int userId) {
        return Jwts.builder()
                .setSubject("student@example.com")
                .claim("role", "STUDENT")
                .claim("userId", userId)
                .setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
