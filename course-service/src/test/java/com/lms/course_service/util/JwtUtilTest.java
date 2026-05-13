package com.lms.course_service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret = "testSecretKeyLongEnoughForHmacSha256Algorithm";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
    }

    private String createToken(String role) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void extractRole_success() {
        String token = createToken("INSTRUCTOR");
        assertEquals("INSTRUCTOR", jwtUtil.extractRole(token));
    }

    @Test
    void isTokenValid_valid_returnsTrue() {
        String token = createToken("STUDENT");
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_invalid_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("invalid.token.here"));
    }
}
