package com.lms.lesson_service.service.impl;

import com.lms.lesson_service.dto.AiGenerationResult;
import com.lms.lesson_service.dto.AskAiRequest;
import com.lms.lesson_service.dto.AskAiResponse;
import com.lms.lesson_service.dto.LessonRequestDto;
import com.lms.lesson_service.dto.LessonResponseDto;
import com.lms.lesson_service.entity.Lesson;
import com.lms.lesson_service.mapper.LessonMapper;
import com.lms.lesson_service.repository.LessonRepository;
import com.lms.lesson_service.service.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private LessonServiceImpl lessonService;

    private Lesson mockLesson;
    private LessonRequestDto requestDto;
    private LessonResponseDto responseDto;

    @BeforeEach
    void setUp() {
        // Using builder since entity has @Builder
        mockLesson = Lesson.builder()
                .lessonId(1)
                .courseId(10)
                .title("Introduction to Java")
                .description("Learn Java basics")
                .videoUrl("http://video.url/java.mp4")
                .resourceUrl("http://resource.url/java.pdf")
                .orderIndex(1)
                .durationMinutes(30)
                .published(false)
                .preview(false)
                .build();

        requestDto = new LessonRequestDto();
        requestDto.setCourseId(10);
        requestDto.setTitle("Introduction to Java");
        requestDto.setDescription("Learn Java basics");
        requestDto.setVideoUrl("http://video.url/java.mp4");
        requestDto.setResourceUrl("http://resource.url/java.pdf");
        requestDto.setOrderIndex(1);
        requestDto.setDurationMinutes(30);
        requestDto.setPreview(false);

        responseDto = new LessonResponseDto();
        responseDto.setLessonId(1);
        responseDto.setTitle("Introduction to Java");
        responseDto.setCourseId(10);
        responseDto.setPublished(false);
        responseDto.setPreview(false);
    }

    // ==================== CREATE LESSON ====================

    @Test
    void createLesson_success() {
        try (MockedStatic<LessonMapper> mapper = mockStatic(LessonMapper.class)) {
            mapper.when(() -> LessonMapper.toEntity(requestDto)).thenReturn(mockLesson);
            mapper.when(() -> LessonMapper.toResponseDto(mockLesson)).thenReturn(responseDto);
            when(lessonRepository.save(any(Lesson.class))).thenReturn(mockLesson);

            LessonResponseDto result = lessonService.createLesson(requestDto);

            assertNotNull(result);
            assertEquals("Introduction to Java", result.getTitle());
            assertFalse(mockLesson.getPublished()); // always false on create
            verify(lessonRepository, times(1)).save(any(Lesson.class));
        }
    }

    // ==================== GET LESSON BY ID ====================

    @Test
    void getLessonById_success() {
        try (MockedStatic<LessonMapper> mapper = mockStatic(LessonMapper.class)) {
            mapper.when(() -> LessonMapper.toResponseDto(mockLesson)).thenReturn(responseDto);
            when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));

            LessonResponseDto result = lessonService.getLessonById(1);

            assertNotNull(result);
            assertEquals(1, result.getLessonId());
            verify(lessonRepository, times(1)).findById(1);
        }
    }

    @Test
    void getLessonById_notFound_throwsException() {
        when(lessonRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> lessonService.getLessonById(99));
    }

    // ==================== GET LESSONS BY COURSE ====================

    @Test
    void getLessonsByCourse_success() {
        try (MockedStatic<LessonMapper> mapper = mockStatic(LessonMapper.class)) {
            mapper.when(() -> LessonMapper.toResponseDto(mockLesson)).thenReturn(responseDto);
            when(lessonRepository.findByCourseIdOrderByOrderIndexAsc(10))
                    .thenReturn(List.of(mockLesson));

            List<LessonResponseDto> result = lessonService.getLessonsByCourse(10);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(lessonRepository, times(1))
                    .findByCourseIdOrderByOrderIndexAsc(10);
        }
    }

    @Test
    void getLessonsByCourse_empty() {
        when(lessonRepository.findByCourseIdOrderByOrderIndexAsc(99))
                .thenReturn(List.of());

        List<LessonResponseDto> result = lessonService.getLessonsByCourse(99);

        assertTrue(result.isEmpty());
    }

    // ==================== GET PUBLISHED LESSONS ====================

    @Test
    void getPublishedLessonsByCourse_success() {
        mockLesson = Lesson.builder()
                .lessonId(1)
                .courseId(10)
                .title("Introduction to Java")
                .published(true)
                .preview(false)
                .orderIndex(1)
                .build();

        try (MockedStatic<LessonMapper> mapper = mockStatic(LessonMapper.class)) {
            mapper.when(() -> LessonMapper.toResponseDto(mockLesson)).thenReturn(responseDto);
            when(lessonRepository.findByCourseIdAndPublishedTrueOrderByOrderIndexAsc(10))
                    .thenReturn(List.of(mockLesson));

            List<LessonResponseDto> result = lessonService.getPublishedLessonsByCourse(10);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    void getPublishedLessonsByCourse_empty() {
        when(lessonRepository.findByCourseIdAndPublishedTrueOrderByOrderIndexAsc(99))
                .thenReturn(List.of());

        List<LessonResponseDto> result = lessonService.getPublishedLessonsByCourse(99);

        assertTrue(result.isEmpty());
    }

    // ==================== GET PREVIEW LESSONS ====================

    @Test
    void getPreviewLessonsByCourse_success() {
        mockLesson = Lesson.builder()
                .lessonId(1)
                .courseId(10)
                .title("Introduction to Java")
                .published(false)
                .preview(true)
                .orderIndex(1)
                .build();

        try (MockedStatic<LessonMapper> mapper = mockStatic(LessonMapper.class)) {
            mapper.when(() -> LessonMapper.toResponseDto(mockLesson)).thenReturn(responseDto);
            when(lessonRepository.findByCourseIdAndPreviewTrueOrderByOrderIndexAsc(10))
                    .thenReturn(List.of(mockLesson));

            List<LessonResponseDto> result = lessonService.getPreviewLessonsByCourse(10);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    void getPreviewLessonsByCourse_empty() {
        when(lessonRepository.findByCourseIdAndPreviewTrueOrderByOrderIndexAsc(99))
                .thenReturn(List.of());

        List<LessonResponseDto> result = lessonService.getPreviewLessonsByCourse(99);

        assertTrue(result.isEmpty());
    }

    // ==================== UPDATE LESSON ====================

    @Test
    void updateLesson_success() {
        try (MockedStatic<LessonMapper> mapper = mockStatic(LessonMapper.class)) {
            mapper.when(() -> LessonMapper.toResponseDto(mockLesson)).thenReturn(responseDto);
            when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));
            when(lessonRepository.save(any(Lesson.class))).thenReturn(mockLesson);

            LessonResponseDto result = lessonService.updateLesson(1, requestDto);

            assertNotNull(result);
            verify(lessonRepository, times(1)).save(any(Lesson.class));
        }
    }

    @Test
    void updateLesson_notFound_throwsException() {
        when(lessonRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> lessonService.updateLesson(99, requestDto));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_withPreviewTrue_setsPreview() {
        requestDto.setPreview(true);
        try (MockedStatic<LessonMapper> mapper = mockStatic(LessonMapper.class)) {
            mapper.when(() -> LessonMapper.toResponseDto(mockLesson)).thenReturn(responseDto);
            when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));
            when(lessonRepository.save(any(Lesson.class))).thenReturn(mockLesson);

            lessonService.updateLesson(1, requestDto);

            assertTrue(mockLesson.getPreview()); // Boolean field from Lesson entity
        }
    }

    // ==================== PUBLISH LESSON ====================

    @Test
    void publishLesson_unpublished_publishes_sendsRabbitEvent() {
        mockLesson = Lesson.builder()
                .lessonId(1)
                .courseId(10)
                .title("Introduction to Java")
                .published(false)
                .preview(false)
                .orderIndex(1)
                .build();

        when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(mockLesson);
        doNothing().when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        try (MockedStatic<LessonMapper> mapper = mockStatic(LessonMapper.class)) {
            mapper.when(() -> LessonMapper.toResponseDto(mockLesson)).thenReturn(responseDto);

            LessonResponseDto result = lessonService.publishLesson(1);

            assertNotNull(result);
            assertTrue(mockLesson.getPublished());
            verify(rabbitTemplate, times(1))
                    .convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    @Test
    void publishLesson_published_unpublishes_noRabbitEvent() {
        mockLesson = Lesson.builder()
                .lessonId(1)
                .courseId(10)
                .title("Introduction to Java")
                .published(true)
                .preview(false)
                .orderIndex(1)
                .build();

        when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(mockLesson);

        try (MockedStatic<LessonMapper> mapper = mockStatic(LessonMapper.class)) {
            mapper.when(() -> LessonMapper.toResponseDto(mockLesson)).thenReturn(responseDto);

            lessonService.publishLesson(1);

            assertFalse(mockLesson.getPublished());
            verify(rabbitTemplate, never())
                    .convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    @Test
    void publishLesson_notFound_throwsException() {
        when(lessonRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> lessonService.publishLesson(99));
    }

    // ==================== DELETE LESSON ====================

    @Test
    void deleteLesson_success() {
        when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));
        doNothing().when(lessonRepository).delete(mockLesson);

        lessonService.deleteLesson(1);

        verify(lessonRepository, times(1)).delete(mockLesson);
    }

    @Test
    void deleteLesson_notFound_throwsException() {
        when(lessonRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> lessonService.deleteLesson(99));
        verify(lessonRepository, never()).delete(any());
    }

    // ==================== TOTAL LESSONS COUNT ====================

    @Test
    void getTotalLessonsCount_success() {
        when(lessonRepository.count()).thenReturn(42L);

        long result = lessonService.getTotalLessonsCount();

        assertEquals(42L, result);
        verify(lessonRepository, times(1)).count();
    }

    @Test
    void getTotalLessonsCount_zero() {
        when(lessonRepository.count()).thenReturn(0L);

        long result = lessonService.getTotalLessonsCount();

        assertEquals(0L, result);
    }

    // ==================== ASK AI ====================

    @Test
    void askAi_success() {
        when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));

        AiGenerationResult aiResult = new AiGenerationResult();
        aiResult.setResponse("Java is a programming language.");
        aiResult.setSuccess(true);

        when(geminiService.generateResponse(anyString())).thenReturn(aiResult);

        AskAiRequest request = new AskAiRequest();
        request.setQuestion("What is Java?");

        AskAiResponse result = lessonService.askAi(1, request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Java is a programming language.", result.getResponse());
        verify(geminiService, times(1)).generateResponse(anyString());
    }

    @Test
    void askAi_lessonNotFound_throwsException() {
        when(lessonRepository.findById(99)).thenReturn(Optional.empty());

        AskAiRequest request = new AskAiRequest();
        request.setQuestion("What is OOP?");

        assertThrows(RuntimeException.class,
                () -> lessonService.askAi(99, request));
        verify(geminiService, never()).generateResponse(anyString());
    }

    @Test
    void askAi_geminiReturnsFailure() {
        when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));

        AiGenerationResult failedResult = new AiGenerationResult();
        failedResult.setResponse("Service unavailable");
        failedResult.setSuccess(false);

        when(geminiService.generateResponse(anyString())).thenReturn(failedResult);

        AskAiRequest request = new AskAiRequest();
        request.setQuestion("Explain OOP");

        AskAiResponse result = lessonService.askAi(1, request);

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    void summarizeLesson_success() {
        when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));
        AiGenerationResult aiResult = new AiGenerationResult();
        aiResult.setResponse("Summary");
        aiResult.setSuccess(true);
        when(geminiService.generateResponse(anyString())).thenReturn(aiResult);

        AskAiResponse result = lessonService.summarizeLesson(1);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(geminiService).generateResponse(contains("comprehensive summary"));
    }

    @Test
    void generateQuiz_success() {
        when(lessonRepository.findById(1)).thenReturn(Optional.of(mockLesson));
        AiGenerationResult aiResult = new AiGenerationResult();
        aiResult.setResponse("Quiz");
        aiResult.setSuccess(true);
        when(geminiService.generateResponse(anyString())).thenReturn(aiResult);

        AskAiResponse result = lessonService.generateQuiz(1);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(geminiService).generateResponse(contains("Generate a quiz"));
    }
}