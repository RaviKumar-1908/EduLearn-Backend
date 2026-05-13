package com.lms.progress.resource;

import com.lms.progress.entity.Certificate;
import com.lms.progress.entity.Progress;
import com.lms.progress.service.ProgressService;
import com.lms.progress.security.JwtFilter;
import com.lms.progress.security.JwtUtil;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProgressResource.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgressResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProgressService progressService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtFilter jwtFilter;

    private Progress progress;
    private Certificate certificate;

    @BeforeEach
    void setUp() {
        progress = new Progress();
        progress.setProgressId(1);
        progress.setStudentId(10);
        progress.setCourseId(101);
        progress.setLessonId(201);

        certificate = new Certificate();
        certificate.setCertificateId(1);
        certificate.setStudentId(10);
        certificate.setCourseId(101);
        certificate.setVerificationCode("CERT-123");
    }

    @Test
    void track_success() throws Exception {
        doNothing().when(progressService).trackProgress(anyInt(), anyInt(), anyInt(), anyInt());

        mockMvc.perform(post("/api/progress/track")
                .param("studentId", "10")
                .param("courseId", "101")
                .param("lessonId", "201")
                .param("watchedSeconds", "120"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void track_negativeSeconds_failure() throws Exception {
        mockMvc.perform(post("/api/progress/track")
                .param("studentId", "10")
                .param("courseId", "101")
                .param("lessonId", "201")
                .param("watchedSeconds", "-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void markComplete_success() throws Exception {
        doNothing().when(progressService).markLessonComplete(anyInt(), anyInt(), anyInt());

        mockMvc.perform(post("/api/progress/complete")
                .param("studentId", "10")
                .param("courseId", "101")
                .param("lessonId", "201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void getCourseProgress_success() throws Exception {
        when(progressService.getCourseProgress(10, 101)).thenReturn(75);

        mockMvc.perform(get("/api/progress/course")
                .param("studentId", "10")
                .param("courseId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(75));
    }

    @Test
    void getLessonProgress_success() throws Exception {
        when(progressService.getLessonProgress(10, 201)).thenReturn(Optional.of(progress));

        mockMvc.perform(get("/api/progress/lesson")
                .param("studentId", "10")
                .param("lessonId", "201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressId").value(1));
    }

    @Test
    void getLessonProgress_notFound() throws Exception {
        when(progressService.getLessonProgress(10, 201)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/progress/lesson")
                .param("studentId", "10")
                .param("lessonId", "201"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllProgressByStudent_success() throws Exception {
        when(progressService.getAllProgressByStudent(10)).thenReturn(List.of(progress));

        mockMvc.perform(get("/api/progress/student").param("studentId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void issueCertificate_success() throws Exception {
        when(progressService.issueCertificate(anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(certificate);

        mockMvc.perform(post("/api/progress/certificates/issue")
                .param("studentId", "10")
                .param("courseId", "101")
                .param("courseName", "Java"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.verificationCode").value("CERT-123"));
    }

    @Test
    void getCertificate_success() throws Exception {
        when(progressService.getCertificate(10, 101)).thenReturn(Optional.of(certificate));

        mockMvc.perform(get("/api/progress/certificates")
                .param("studentId", "10")
                .param("courseId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationCode").value("CERT-123"));
    }

    @Test
    void getCertificate_notFound() throws Exception {
        when(progressService.getCertificate(10, 101)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/progress/certificates")
                .param("studentId", "10")
                .param("courseId", "101"))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyCertificate_success() throws Exception {
        when(progressService.verifyCertificate("CERT-123")).thenReturn(certificate);

        mockMvc.perform(get("/api/progress/certificates/verify").param("code", "CERT-123"))
                .andExpect(status().isOk());
    }
}
