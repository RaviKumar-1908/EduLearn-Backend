package com.lms.auth_service.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret = "testSecretKeyLongEnoughForHmacSha256Algorithm";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "tokenExpiry", 3600000L);
    }

    @Test
    void generateToken_success() {
        String token = jwtUtil.generateToken(1, "john@example.com", "STUDENT", "John Doe", "pic", "MALE");
        assertNotNull(token);
        assertEquals("john@example.com", jwtUtil.extractEmail(token));
        assertEquals("STUDENT", jwtUtil.extractRole(token));
    }

    @Test
    void validateToken_valid_returnsTrue() {
        String token = jwtUtil.generateToken(1, "john@example.com", "STUDENT", "John Doe", "pic", "MALE");
        assertTrue(jwtUtil.validateToken(token, "john@example.com"));
    }

    @Test
    void validateToken_invalidEmail_returnsFalse() {
        String token = jwtUtil.generateToken(1, "john@example.com", "STUDENT", "John Doe", "pic", "MALE");
        assertFalse(jwtUtil.validateToken(token, "wrong@example.com"));
    }

    @Test
    void isTokenExpired_notExpired_returnsFalse() {
        String token = jwtUtil.generateToken(1, "john@example.com", "STUDENT", "John Doe", "pic", "MALE");
        // isTokenExpired is private, but validateToken uses it
        assertTrue(jwtUtil.validateToken(token, "john@example.com"));
    }
}
