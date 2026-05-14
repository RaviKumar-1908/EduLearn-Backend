package com.lms.course_service.controller;

import com.lms.course_service.dto.CourseRequestDto;
import com.lms.course_service.dto.CourseResponseDto;
import com.lms.course_service.service.CourseService;
import com.lms.course_service.config.JwtAuthenticationFilter;
import com.lms.course_service.util.JwtUtil;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private CourseResponseDto responseDto;
    private CourseRequestDto requestDto;

    @BeforeEach
    void setUp() {
        responseDto = new CourseResponseDto();
        responseDto.setCourseId(1);
        responseDto.setTitle("Java");

        requestDto = new CourseRequestDto();
        requestDto.setTitle("Java");
        requestDto.setCategory("IT");
        requestDto.setLevel("BEGINNER");
        requestDto.setPrice(99.99);
        requestDto.setInstructorId(10);
    }

    @Test
    void createCourse_success() throws Exception {
        when(courseService.createCourse(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/course")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(1));
    }

    @Test
    void getAllCourses_success() throws Exception {
        when(courseService.getAllCourses()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/course"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCourseById_success() throws Exception {
        when(courseService.getCourseById(1)).thenReturn(responseDto);

        mockMvc.perform(get("/api/course/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(1));
    }

    @Test
    void searchCourses_success() throws Exception {
        when(courseService.searchCourses("Java")).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/course/search").param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void publishCourse_success() throws Exception {
        when(courseService.publishCourse(1)).thenReturn(responseDto);

        mockMvc.perform(put("/api/course/publish/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCourse_success() throws Exception {
        doNothing().when(courseService).deleteCourse(1);

        mockMvc.perform(delete("/api/course/1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateCourse_success() throws Exception {
        when(courseService.updateCourse(anyInt(), any())).thenReturn(responseDto);

        mockMvc.perform(put("/api/course/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateCourseStatus_success() throws Exception {
        when(courseService.updateCourseStatus(anyInt(), anyString())).thenReturn(responseDto);

        mockMvc.perform(put("/api/course/admin/status/1").param("status", "APPROVED"))
                .andExpect(status().isOk());
    }

    @Test
    void getCoursesByCategory_success() throws Exception {
        when(courseService.getCoursesByCategory("IT")).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/course/category/IT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getFeaturedCourses_success() throws Exception {
        when(courseService.getFeaturedCourses()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/course/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getCoursesByInstructor_success() throws Exception {
        when(courseService.getCoursesByInstructor(10)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/course/instructor/10"))
                .andExpect(status().isOk());
    }

    @Test
    void getCoursesByLevel_success() throws Exception {
        when(courseService.getCoursesByLevel("BEGINNER")).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/course/level/BEGINNER"))
                .andExpect(status().isOk());
    }

    @Test
    void getCoursesByPrice_success() throws Exception {
        when(courseService.getCoursesByPrice(100.0)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/course/price").param("maxPrice", "100.0"))
                .andExpect(status().isOk());
    }

    @Test
    void getCoursesByIds_success() throws Exception {
        when(courseService.getCoursesByIds(anyList())).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/course/bulk").param("ids", "1,2,3"))
                .andExpect(status().isOk());
    }

    @Test
    void getAdminStats_success() throws Exception {
        when(courseService.getAdminStats()).thenReturn(Map.of("total", 10));

        mockMvc.perform(get("/api/course/admin/stats"))
                .andExpect(status().isOk());
    }
}
