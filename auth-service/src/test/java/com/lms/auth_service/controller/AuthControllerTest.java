package com.lms.auth_service.controller;

import com.lms.auth_service.dto.*;
import com.lms.auth_service.entity.User;
import com.lms.auth_service.enums.Role;
import com.lms.auth_service.service.AuthService;
import com.lms.auth_service.util.JwtUtil;
import com.lms.auth_service.config.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Security Mocks
    @MockBean
    private JwtUtil jwtUtil;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @MockBean
    private CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    
    @MockBean
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;
    
    @MockBean
    private CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setEmail("test@example.com");
        mockUser.setFullName("Test User");
        mockUser.setRole(Role.STUDENT);
    }

    @Test
    void register_success() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setFullName("Test User");
        request.setRole("STUDENT");

        when(authService.register(any())).thenReturn(mockUser);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void login_success() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        AuthResponseDto response = AuthResponseDto.builder()
                .token("token123")
                .userId(1)
                .email("test@example.com")
                .fullName("Test User")
                .role("STUDENT")
                .build();

        when(authService.login(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"));
    }

    @Test
    void getProfile_success() throws Exception {
        when(authService.getUserById(1)).thenReturn(mockUser);

        mockMvc.perform(get("/auth/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void logout_success() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_success() throws Exception {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
        request.setEmail("test@example.com");

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void verifyOtp_success() throws Exception {
        when(authService.verifyOtp(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\", \"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getAllUsers_success() throws Exception {
        when(authService.getAllUsers()).thenReturn(List.of(mockUser));

        mockMvc.perform(get("/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
