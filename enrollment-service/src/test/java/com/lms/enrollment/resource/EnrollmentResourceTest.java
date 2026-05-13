package com.lms.enrollment.resource;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.service.EnrollmentService;
import com.lms.enrollment.config.JwtAuthenticationFilter;
import com.lms.enrollment.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnrollmentResource.class)
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnrollmentService enrollmentService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        enrollment = new Enrollment();
        enrollment.setEnrollmentId(1);
        enrollment.setStudentId(10);
        enrollment.setCourseId(101);
    }

    @Test
    void enroll_success() throws Exception {
        when(enrollmentService.enroll(anyInt(), anyInt(), anyDouble())).thenReturn(enrollment);

        mockMvc.perform(post("/api/enrollment/enroll")
                .param("studentId", "10")
                .param("courseId", "101")
                .param("price", "99.99"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").value(1));
    }

    @Test
    void enroll_conflict() throws Exception {
        when(enrollmentService.enroll(anyInt(), anyInt(), anyDouble())).thenThrow(new IllegalStateException("Already enrolled"));

        mockMvc.perform(post("/api/enrollment/enroll")
                .param("studentId", "10")
                .param("courseId", "101"))
                .andExpect(status().isConflict());
    }

    @Test
    void unenroll_success() throws Exception {
        doNothing().when(enrollmentService).unenroll(10, 101);

        mockMvc.perform(delete("/api/enrollment/unenroll")
                .param("studentId", "10")
                .param("courseId", "101"))
                .andExpect(status().isNoContent());
    }

    @Test
    void unenroll_notFound() throws Exception {
        doThrow(new IllegalArgumentException("Not found")).when(enrollmentService).unenroll(anyInt(), anyInt());

        mockMvc.perform(delete("/api/enrollment/unenroll")
                .param("studentId", "10")
                .param("courseId", "101"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByStudent_success() throws Exception {
        when(enrollmentService.getEnrollmentsByStudent(10)).thenReturn(List.of(enrollment));

        mockMvc.perform(get("/api/enrollment/student/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getStudentDashboard_success() throws Exception {
        when(enrollmentService.getEnrollmentsByStudent(10)).thenReturn(List.of(enrollment));

        mockMvc.perform(get("/api/enrollment/dashboard/student/10"))
                .andExpect(status().isOk());
    }

    @Test
    void getByCourse_success() throws Exception {
        when(enrollmentService.getEnrollmentsByCourse(101)).thenReturn(List.of(enrollment));

        mockMvc.perform(get("/api/enrollment/course/101"))
                .andExpect(status().isOk());
    }

    @Test
    void getCourseDashboard_success() throws Exception {
        when(enrollmentService.getEnrollmentsByCourse(101)).thenReturn(List.of(enrollment));

        mockMvc.perform(get("/api/enrollment/dashboard/course/101"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProgress_success() throws Exception {
        doNothing().when(enrollmentService).updateProgress(10, 101, 75);

        mockMvc.perform(put("/api/enrollment/progress")
                .param("studentId", "10")
                .param("courseId", "101")
                .param("progressPercent", "75"))
                .andExpect(status().isOk());
    }

    @Test
    void markComplete_success() throws Exception {
        doNothing().when(enrollmentService).markComplete(10, 101);

        mockMvc.perform(put("/api/enrollment/complete")
                .param("studentId", "10")
                .param("courseId", "101"))
                .andExpect(status().isOk());
    }

    @Test
    void isEnrolled_success() throws Exception {
        when(enrollmentService.isEnrolled(10, 101)).thenReturn(true);

        mockMvc.perform(get("/api/enrollment/isEnrolled")
                .param("studentId", "10")
                .param("courseId", "101"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void issueCertificate_success() throws Exception {
        doNothing().when(enrollmentService).issueCertificate(10, 101);

        mockMvc.perform(post("/api/enrollment/certificate")
                .param("studentId", "10")
                .param("courseId", "101"))
                .andExpect(status().isOk());
    }

    @Test
    void getEnrollmentCount_success() throws Exception {
        when(enrollmentService.getEnrollmentCount(101)).thenReturn(5);

        mockMvc.perform(get("/api/enrollment/count/101"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void getEnrollmentCounts_success() throws Exception {
        when(enrollmentService.getEnrollmentCounts(anyList())).thenReturn(Map.of(101, 5));

        mockMvc.perform(get("/api/enrollment/counts").param("courseIds", "101"))
                .andExpect(status().isOk());
    }

    @Test
    void getAdminStats_success() throws Exception {
        when(enrollmentService.getAdminStats()).thenReturn(Map.of("total", 100));

        mockMvc.perform(get("/api/enrollment/admin/stats"))
                .andExpect(status().isOk());
    }
}
