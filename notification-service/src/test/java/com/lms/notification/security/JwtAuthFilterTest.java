package com.lms.notification.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_knownPublicPaths() {
        assertTrue(filter.shouldNotFilter(requestFor("/swagger-ui/index.html")));
        assertTrue(filter.shouldNotFilter(requestFor("/v3/api-docs")));
        assertTrue(filter.shouldNotFilter(requestFor("/actuator/health")));
        assertTrue(filter.shouldNotFilter(requestFor("/ws-notifications/info")));
        assertFalse(filter.shouldNotFilter(requestFor("/api/notification/my")));
    }

    @Test
    void doFilterInternal_proceedsOnMissingHeader() throws Exception {
        MockHttpServletRequest request = requestFor("/api/notification/my");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus()); // Not manually rejected
        verifyNoInteractions(jwtUtil);
        verify(filterChain).doFilter(request, response); // PROCEEDED
    }

    @Test
    void doFilterInternal_proceedsOnOptionsRequest() throws Exception {
        MockHttpServletRequest request = requestFor("/api/notification/my");
        request.setMethod("OPTIONS");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verifyNoInteractions(jwtUtil);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_rejectsInvalidToken() throws Exception {
        MockHttpServletRequest request = requestFor("/api/notification/my");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.isTokenValid("bad-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("JWT token is invalid or expired"));
        verify(jwtUtil).isTokenValid("bad-token");
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_setsAuthenticationAndContinues() throws Exception {
        MockHttpServletRequest request = requestFor("/api/notification/my");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("student@example.com");
        when(jwtUtil.extractRole("valid-token")).thenReturn("student");
        when(jwtUtil.extractUserId("valid-token")).thenReturn(10);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("student@example.com", authentication.getPrincipal());
        assertEquals("ROLE_STUDENT", authentication.getAuthorities().iterator().next().getAuthority());
        assertInstanceOf(Map.class, authentication.getDetails());
        assertEquals(10, ((Map<?, ?>) authentication.getDetails()).get("userId"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_keepsExistingAuthentication() throws Exception {
        MockHttpServletRequest request = requestFor("/api/notification/my");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("student@example.com");
        when(jwtUtil.extractRole("valid-token")).thenReturn("");
        when(jwtUtil.extractUserId("valid-token")).thenReturn(10);

        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("existing", null));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("existing", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest requestFor(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
