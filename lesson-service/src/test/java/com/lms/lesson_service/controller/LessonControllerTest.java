package com.lms.lesson_service.controller;

import com.lms.lesson_service.dto.LessonRequestDto;
import com.lms.lesson_service.dto.LessonResponseDto;
import com.lms.lesson_service.dto.AskAiRequest;
import com.lms.lesson_service.dto.AskAiResponse;
import com.lms.lesson_service.service.LessonService;
import com.lms.lesson_service.config.JwtAuthenticationFilter;
import com.lms.lesson_service.util.JwtUtil;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LessonController.class)
@AutoConfigureMockMvc(addFilters = false)
class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LessonService lessonService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private LessonResponseDto responseDto;
    private LessonRequestDto requestDto;

    @BeforeEach
    void setUp() {
        responseDto = new LessonResponseDto();
        responseDto.setLessonId(1);
        responseDto.setTitle("Java Intro");
        responseDto.setCourseId(101);

        requestDto = new LessonRequestDto();
        requestDto.setTitle("Java Intro");
        requestDto.setCourseId(101);
        requestDto.setOrderIndex(1);
        requestDto.setDurationMinutes(10);
    }

    @Test
    void createLesson_success() throws Exception {
        when(lessonService.createLesson(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/lesson")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessonId").value(1));
    }

    @Test
    void getLessonsByCourse_success() throws Exception {
        when(lessonService.getLessonsByCourse(101)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/lesson/course/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getLessonsByCourse_empty() throws Exception {
        when(lessonService.getLessonsByCourse(999)).thenReturn(List.of());

        mockMvc.perform(get("/api/lesson/course/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getLessonById_success() throws Exception {
        when(lessonService.getLessonById(1)).thenReturn(responseDto);

        mockMvc.perform(get("/api/lesson/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(1));
    }

    @Test
    void getLessonById_notFound() throws Exception {
        when(lessonService.getLessonById(99)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/lesson/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLesson_success() throws Exception {
        when(lessonService.updateLesson(eq(1), any())).thenReturn(responseDto);

        mockMvc.perform(put("/api/lesson/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteLesson_success() throws Exception {
        doNothing().when(lessonService).deleteLesson(1);

        mockMvc.perform(delete("/api/lesson/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void askAi_success() throws Exception {
        AskAiRequest request = new AskAiRequest();
        request.setQuestion("What is Java?");
        
        AskAiResponse response = new AskAiResponse();
        response.setResponse("AI Answer");
        
        when(lessonService.askAi(eq(1), any(AskAiRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/lesson/1/ask-ai")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("AI Answer"));
    }

    @Test
    void summarize_success() throws Exception {
        AskAiResponse response = new AskAiResponse();
        response.setResponse("Summary of lesson");
        
        when(lessonService.summarizeLesson(1)).thenReturn(response);

        mockMvc.perform(get("/api/lesson/1/summarize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Summary of lesson"));
    }

    @Test
    void generateQuiz_success() throws Exception {
        AskAiResponse response = new AskAiResponse();
        response.setResponse("Quiz questions...");
        
        when(lessonService.generateQuiz(1)).thenReturn(response);

        mockMvc.perform(get("/api/lesson/1/generate-quiz"))
                .andExpect(status().isOk());
    }
}
