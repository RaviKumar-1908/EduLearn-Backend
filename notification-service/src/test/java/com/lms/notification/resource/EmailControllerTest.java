package com.lms.notification.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.notification.dto.EmailRequest;
import com.lms.notification.security.JwtAuthFilter;
import com.lms.notification.security.JwtUtil;
import com.lms.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void sendManualWelcome_success() throws Exception {
        mockMvc.perform(post("/api/notification/email/welcome")
                        .param("email", "welcome@example.com")
                        .param("name", "Ravi"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome email queued successfully"));

        verify(notificationService).sendWelcomeEmail("welcome@example.com", "Ravi");
    }

    @Test
    void sendManualOtp_success() throws Exception {
        mockMvc.perform(post("/api/notification/email/otp")
                        .param("email", "otp@example.com")
                        .param("otp", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP email queued successfully"));

        verify(notificationService).sendOtpEmail("otp@example.com", "123456");
    }

    @Test
    void sendCustomEmail_success() throws Exception {
        EmailRequest request = EmailRequest.builder()
                .to("custom@example.com")
                .subject("Hello")
                .templateName("generic")
                .templateModel(Map.of("name", "Ravi"))
                .build();

        mockMvc.perform(post("/api/notification/email/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Custom email queued successfully"));

        verify(notificationService).sendEmailAlert("custom@example.com", "Hello", "Custom content");
    }

    @Test
    void sendCertificateEmail_success() throws Exception {
        EmailRequest request = EmailRequest.builder()
                .to("certificate@example.com")
                .subject("Certificate")
                .templateName("certificate")
                .build();

        mockMvc.perform(post("/api/notification/email/certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Certificate email sent successfully"));

        verify(notificationService).sendCertificateEmail(org.mockito.ArgumentMatchers.any(EmailRequest.class));
    }
}
