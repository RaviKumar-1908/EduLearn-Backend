package com.lms.lesson_service.service;

import com.lms.lesson_service.dto.AiGenerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(geminiService, "apiKey", "gsk_test_api_key");
        ReflectionTestUtils.setField(geminiService, "restTemplate", restTemplate);
    }

    @Test
    void generateResponse_success() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        
        message.put("role", "assistant");
        message.put("content", "Hello from AI");
        choice.put("message", message);
        response.put("choices", List.of(choice));

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        AiGenerationResult result = geminiService.generateResponse("Hi");

        assertTrue(result.isSuccess());
        assertEquals("Hello from AI", result.getResponse());
    }

    @Test
    void generateResponse_noApiKey_returnsUnavailable() {
        ReflectionTestUtils.setField(geminiService, "apiKey", "");
        
        AiGenerationResult result = geminiService.generateResponse("Hi");

        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("unavailable"));
    }

    @Test
    void generateResponse_emptyResponse_returnsError() {
        Map<String, Object> response = new HashMap<>();
        response.put("choices", Collections.emptyList());

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(response);

        AiGenerationResult result = geminiService.generateResponse("Hi");

        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("could not generate"));
    }

    @Test
    void generateResponse_apiError_returnsUnavailable() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("API Down"));

        AiGenerationResult result = geminiService.generateResponse("Hi");

        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("unavailable"));
    }

    @Test
    void extractTextFromResponse_nullResponse_returnsNull() {
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(null);
        
        AiGenerationResult result = geminiService.generateResponse("Hi");
        assertFalse(result.isSuccess());
        assertTrue(result.getResponse().contains("could not generate"));
    }
}
