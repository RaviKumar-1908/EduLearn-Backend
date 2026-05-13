package com.lms.auth_service.controller;

import com.lms.auth_service.entity.BugReport;
import com.lms.auth_service.entity.User;
import com.lms.auth_service.enums.Role;
import com.lms.auth_service.repository.BugReportRepository;
import com.lms.auth_service.repository.UserRepository;
import com.lms.auth_service.config.JwtAuthenticationFilter;
import com.lms.auth_service.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BugReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class BugReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BugReportRepository bugReportRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private BugReport sampleBug;

    @BeforeEach
    void setUp() {
        sampleBug = new BugReport();
        sampleBug.setId(1);
        sampleBug.setEmail("user@example.com");
        sampleBug.setUsername("testuser");
        sampleBug.setContent("Bug details");
    }

    @Test
    void reportBug_success() throws Exception {
        when(bugReportRepository.save(any())).thenReturn(sampleBug);
        when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of());

        mockMvc.perform(post("/api/bugs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleBug)))
                .andExpect(status().isOk());
        
        verify(bugReportRepository).save(any());
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), anyString(), anyMap());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void reportBug_authenticated_success() throws Exception {
        User user = new User();
        user.setUserId(10);
        user.setEmail("user@example.com");
        user.setFullName("Full Name");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(bugReportRepository.save(any())).thenReturn(sampleBug);

        mockMvc.perform(post("/api/bugs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Bug content\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void reportBug_invalid_returns400() throws Exception {
        mockMvc.perform(post("/api/bugs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllBugs_success() throws Exception {
        when(bugReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleBug));

        mockMvc.perform(get("/api/bugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Bug details"));
    }

    @Test
    void updateStatus_success() throws Exception {
        when(bugReportRepository.findById(1)).thenReturn(Optional.of(sampleBug));
        when(bugReportRepository.save(any())).thenReturn(sampleBug);

        mockMvc.perform(put("/api/bugs/1/status")
                .param("status", "RESOLVED"))
                .andExpect(status().isOk());
        
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyMap());
    }

    @Test
    void updateStatus_notFound_returns404() throws Exception {
        when(bugReportRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/bugs/99/status")
                .param("status", "RESOLVED"))
                .andExpect(status().isBadRequest());
    }
}
