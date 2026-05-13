package com.lms.auth_service.config;

import com.lms.auth_service.entity.User;
import com.lms.auth_service.enums.ApprovalStatus;
import com.lms.auth_service.enums.AuthProvider;
import com.lms.auth_service.enums.Role;
import com.lms.auth_service.repository.UserRepository;
import com.lms.auth_service.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CustomOAuth2SuccessHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private HttpSession session;

    @Mock
    private org.springframework.security.web.RedirectStrategy redirectStrategy;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "redirectUri", "http://localhost:3000/oauth2/redirect");
        handler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    void onAuthenticationSuccess_existingUser_redirectsWithToken() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", "john@example.com", "name", "John Doe"));
        when(oAuth2User.getAttribute("email")).thenReturn("john@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("John Doe");

        User existingUser = User.builder()
                .userId(1)
                .email("john@example.com")
                .fullName("John Doe")
                .role(Role.STUDENT)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any())).thenReturn(existingUser);
        when(jwtUtil.generateToken(anyInt(), anyString(), anyString(), anyString(), any(), any())).thenReturn("mock-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), contains("token=mock-token"));
        verify(rabbitTemplate).convertAndSend(eq("lms.events.exchange"), eq("notification.auth.login"), anyMap());
    }

    @Test
    void onAuthenticationSuccess_newUser_createsUserAndRedirects() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", "new@example.com", "name", "New User"));
        when(oAuth2User.getAttribute("email")).thenReturn("new@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("New User");

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        
        User savedUser = User.builder()
                .userId(2)
                .email("new@example.com")
                .fullName("New User")
                .role(Role.STUDENT)
                .build();
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtUtil.generateToken(anyInt(), anyString(), anyString(), anyString(), any(), any())).thenReturn("new-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).save(any());
        verify(rabbitTemplate).convertAndSend(eq("lms.events.exchange"), eq("notification.auth.register"), anyMap());
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), contains("token=new-token"));
    }

    @Test
    void onAuthenticationSuccess_missingEmail_sendsError() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(null);
        when(oAuth2User.getAttribute("name")).thenReturn("Learner");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    void onAuthenticationSuccess_adminRoleSelected_redirectsWithError() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("admin@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Admin User");
        
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(CustomOAuth2AuthorizationRequestResolver.GOOGLE_SELECTED_ROLE)).thenReturn("ADMIN");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), contains("error="));
    }
}
