package com.lms.progress.service;

import com.lms.progress.entity.Certificate;
import com.lms.progress.entity.Progress;
import com.lms.progress.repository.CertificateRepository;
import com.lms.progress.repository.ProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceImplTest {

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProgressServiceImpl progressService;

    private Progress mockProgress;
    private Certificate mockCertificate;

    @BeforeEach
    void setUp() {
        mockProgress = new Progress();
        mockProgress.setStudentId(1);
        mockProgress.setCourseId(101);
        mockProgress.setLessonId(1001);
        mockProgress.setWatchedSeconds(120);
        mockProgress.setCompleted(false);

        mockCertificate = new Certificate();
        mockCertificate.setStudentId(1);
        mockCertificate.setCourseId(101);
        mockCertificate.setVerificationCode("v-code-123");
    }

    @Test
    void trackProgress_success() {
        when(progressRepository.findByStudentIdAndCourseIdAndLessonId(1, 101, 1001))
                .thenReturn(Optional.of(mockProgress));
        when(progressRepository.save(any(Progress.class))).thenReturn(mockProgress);

        progressService.trackProgress(1, 101, 1001, 60);

        assertEquals(180, mockProgress.getWatchedSeconds());
        verify(progressRepository, times(1)).save(mockProgress);
    }

    @Test
    void markLessonComplete_success() {
        when(progressRepository.findByStudentIdAndCourseIdAndLessonId(1, 101, 1001))
                .thenReturn(Optional.of(mockProgress));
        when(progressRepository.save(any(Progress.class))).thenReturn(mockProgress);

        // Mock getCourseProgress (Internal call)
        when(restTemplate.getForObject(anyString(), eq(Collection.class))).thenReturn(List.of(1));
        when(progressRepository.countCompletedByStudentIdAndCourseId(1, 101)).thenReturn(1L);

        progressService.markLessonComplete(1, 101, 1001);

        assertTrue(mockProgress.isCompleted());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void getCourseProgress_success() {
        when(restTemplate.getForObject(anyString(), eq(Collection.class))).thenReturn(List.of(1, 2));
        when(progressRepository.countCompletedByStudentIdAndCourseId(1, 101)).thenReturn(1L);

        int progress = progressService.getCourseProgress(1, 101);

        assertEquals(50, progress);
    }

    @Test
    void issueCertificate_success() {
        // Mock progress 100%
        when(restTemplate.getForObject(anyString(), eq(Collection.class))).thenReturn(List.of(1));
        when(progressRepository.countCompletedByStudentIdAndCourseId(1, 101)).thenReturn(1L);

        when(certificateRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class))).thenReturn(mockCertificate);

        Certificate result = progressService.issueCertificate(1, 101, "Java", "Instructor", "Beginner", 100);

        assertNotNull(result);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void trackProgress_newRecord_success() {
        when(progressRepository.findByStudentIdAndCourseIdAndLessonId(anyInt(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(progressRepository.save(any(Progress.class))).thenAnswer(i -> i.getArgument(0));

        progressService.trackProgress(1, 101, 2002, 10);

        verify(progressRepository).save(argThat(p -> p.getWatchedSeconds() == 10));
    }

    @Test
    void markLessonComplete_alreadyComplete_returnsEarly() {
        mockProgress.setCompleted(true);
        when(progressRepository.findByStudentIdAndCourseIdAndLessonId(1, 101, 1001))
                .thenReturn(Optional.of(mockProgress));

        progressService.markLessonComplete(1, 101, 1001);

        verify(progressRepository, never()).save(any());
    }

    @Test
    void markLessonComplete_rabbitFailure_stillSaves() {
        when(progressRepository.findByStudentIdAndCourseIdAndLessonId(1, 101, 1001))
                .thenReturn(Optional.of(mockProgress));
        when(progressRepository.save(any())).thenReturn(mockProgress);
        when(restTemplate.getForObject(anyString(), any())).thenReturn(List.of(1));
        doThrow(new RuntimeException("Rabbit error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> progressService.markLessonComplete(1, 101, 1001));
        assertTrue(mockProgress.isCompleted());
    }

    @Test
    void getCourseProgress_lessonServiceError_returnsZero() {
        when(restTemplate.getForObject(anyString(), any())).thenThrow(new RuntimeException("Lesson-service down"));

        int progress = progressService.getCourseProgress(1, 101);
        assertEquals(0, progress);
    }

    @Test
    void getCourseProgress_noLessons_returnsZero() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(List.of());

        int progress = progressService.getCourseProgress(1, 101);
        assertEquals(0, progress);
    }

    @Test
    void getCourseProgress_rounding_returnsCorrect() {
        // 2 out of 3 lessons = 66.66% -> 67%
        when(restTemplate.getForObject(anyString(), eq(Collection.class))).thenReturn(List.of(1, 2, 3));
        when(progressRepository.countCompletedByStudentIdAndCourseId(1, 101)).thenReturn(2L);

        int progress = progressService.getCourseProgress(1, 101);
        assertEquals(67, progress);
    }

    @Test
    void issueCertificate_alreadyIssued_throwsException() {
        when(certificateRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(true);
        assertThrows(com.lms.progress.exception.CertificateAlreadyIssuedException.class,
                () -> progressService.issueCertificate(1, 101, "Java", "Inst", "Beg", 100));
    }

    @Test
    void issueCertificate_notComplete_throwsException() {
        when(certificateRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        // Mock 50% progress
        when(restTemplate.getForObject(anyString(), any())).thenReturn(List.of(1, 2));
        when(progressRepository.countCompletedByStudentIdAndCourseId(1, 101)).thenReturn(1L);

        assertThrows(com.lms.progress.exception.CourseNotCompletedException.class,
                () -> progressService.issueCertificate(1, 101, "Java", "Inst", "Beg", 100));
    }

    @Test
    void verifyCertificate_success() {
        when(certificateRepository.findByVerificationCode("v123")).thenReturn(Optional.of(mockCertificate));
        Certificate result = progressService.verifyCertificate("v123");
        assertNotNull(result);
    }

    @Test
    void verifyCertificate_notFound_throwsException() {
        when(certificateRepository.findByVerificationCode("invalid")).thenReturn(Optional.empty());
        assertThrows(com.lms.progress.exception.ResourceNotFoundException.class,
                () -> progressService.verifyCertificate("invalid"));
    }

    @Test
    void getLessonProgress_success() {
        when(progressRepository.findByStudentIdAndLessonId(1, 1001)).thenReturn(Optional.of(mockProgress));
        Optional<Progress> result = progressService.getLessonProgress(1, 1001);
        assertTrue(result.isPresent());
    }

    @Test
    void getAllProgressByStudent_success() {
        when(progressRepository.findByStudentId(1)).thenReturn(List.of(mockProgress));
        List<Progress> results = progressService.getAllProgressByStudent(1);
        assertFalse(results.isEmpty());
    }
}
