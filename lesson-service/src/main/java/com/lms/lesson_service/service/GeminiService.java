package com.lms.lesson_service.service;

import com.lms.lesson_service.dto.AiGenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
public class GeminiService {

    private static final String UNAVAILABLE_MESSAGE =
            "AI Tutor is currently unavailable. Please try again later.";
    private static final String EMPTY_RESPONSE_MESSAGE =
            "AI Tutor could not generate a response right now. Please try again.";

    /** Use Groq with Llama 3 8B model for extreme speed and reliability */
    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public GeminiService() {
        // Configure sensible connect / read timeouts so requests don't hang
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 10 seconds
        factory.setReadTimeout(60_000);     // 60 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    public AiGenerationResult generateResponse(String prompt) {
        if (!hasConfiguredApiKey()) {
            log.warn("API key is not configured.");
            return AiGenerationResult.builder()
                    .response(UNAVAILABLE_MESSAGE)
                    .success(false)
                    .build();
        }

        int maxRetries = 3;
        int retryDelayMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                // Construct Groq API Request Body (OpenAI format)
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "llama-3.1-8b-instant");
                
                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);
                
                requestBody.put("messages", Collections.singletonList(message));

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                log.debug("Calling Groq API at model llama-3.1-8b-instant (Attempt {}/{})", attempt, maxRetries);
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(GROQ_URL, entity, Map.class);

                String extractedText = extractTextFromResponse(response);
                if (extractedText == null || extractedText.isBlank()) {
                    log.warn("Groq returned empty/null text. Full response: {}", response);
                    return AiGenerationResult.builder()
                            .response(EMPTY_RESPONSE_MESSAGE)
                            .success(false)
                            .build();
                }

                return AiGenerationResult.builder()
                        .response(extractedText)
                        .success(true)
                        .build();

            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable e) {
                log.warn("Groq API is busy (503). Attempt {}/{} failed.", attempt, maxRetries);
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * attempt); // Exponential backoff: 1s, 2s, 3s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Groq API overloaded after {} attempts.", maxRetries);
                }
            } catch (Exception e) {
                log.error("Error calling Groq API: {}", e.getMessage(), e);
                break; // Do not retry on other errors (like 400 Bad Request, etc.)
            }
        }

        return AiGenerationResult.builder()
                .response(UNAVAILABLE_MESSAGE)
                .success(false)
                .build();
    }

    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            if (response == null || !response.containsKey("choices")) return null;
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices.isEmpty()) return null;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                return null;
            }

            Object text = message.get("content");
            return text instanceof String ? (String) text : null;
        } catch (Exception e) {
            log.error("Error parsing Groq response", e);
            return null;
        }
    }

    private boolean hasConfiguredApiKey() {
        return apiKey != null
                && !apiKey.isBlank()
                && !"your_gemini_api_key_here".equalsIgnoreCase(apiKey.trim())
                && apiKey.startsWith("gsk_");
    }
}
