package com.lms.assessment.service;

import com.lms.assessment.entity.Attempt;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.Quiz;
import com.lms.assessment.repository.AttemptRepository;
import com.lms.assessment.repository.QuestionRepository;
import com.lms.assessment.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock
    private QuizRepository quizRepo;

    @Mock
    private QuestionRepository questionRepo;

    @Mock
    private AttemptRepository attemptRepo;

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    private Quiz mockQuiz;
    private Question mockQuestion;
    private Attempt mockAttempt;

    @BeforeEach
    void setUp() {
        mockQuiz = new Quiz();
        mockQuiz.setQuizId(1);
        mockQuiz.setTitle("Java Quiz");
        mockQuiz.setCourseId(101);
        mockQuiz.setPassingScore(60);
        mockQuiz.setMaxAttempts(3);
        mockQuiz.setTimeLimitMinutes(30);
        mockQuiz.setPublished(false);

        mockQuestion = new Question();
        mockQuestion.setQuestionId(1);
        mockQuestion.setQuizId(1);
        mockQuestion.setText("What is Java?");
        mockQuestion.setCorrectAnswer("Language");
        mockQuestion.setMarks(10);
        mockQuestion.setType("MCQ");
        mockQuestion.setOrderIndex(1);

        mockAttempt = new Attempt(1, 10);
        mockAttempt.setAttemptId(500);
        mockAttempt.setScore(0);
        mockAttempt.setPassed(false);
        mockAttempt.setAnswers(new HashMap<>());
    }

    // ==================== CREATE QUIZ ====================

    @Test
    void createQuiz_success() {
        when(quizRepo.save(any(Quiz.class))).thenReturn(mockQuiz);

        Quiz result = assessmentService.createQuiz(mockQuiz);

        assertNotNull(result);
        assertEquals("Java Quiz", result.getTitle());
        verify(quizRepo, times(1)).save(mockQuiz);
    }

    // ==================== GET QUIZZES BY COURSE ====================

    @Test
    void getQuizzesByCourse_success() {
        when(quizRepo.findByCourseId(101)).thenReturn(List.of(mockQuiz));

        List<Quiz> result = assessmentService.getQuizzesByCourse(101);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(quizRepo, times(1)).findByCourseId(101);
    }

    @Test
    void getQuizzesByCourse_empty() {
        when(quizRepo.findByCourseId(999)).thenReturn(List.of());

        List<Quiz> result = assessmentService.getQuizzesByCourse(999);

        assertTrue(result.isEmpty());
    }

    // ==================== PUBLISH QUIZ ====================

    @Test
    void publishQuiz_success() {
        when(quizRepo.findById(1)).thenReturn(Optional.of(mockQuiz));
        when(questionRepo.countByQuizId(1)).thenReturn(5);
        when(quizRepo.save(any(Quiz.class))).thenReturn(mockQuiz);

        assessmentService.publishQuiz(1);

        assertTrue(mockQuiz.isPublished());
        verify(quizRepo, times(1)).save(mockQuiz);
    }

    @Test
    void publishQuiz_noQuestions_throwsException() {
        when(quizRepo.findById(1)).thenReturn(Optional.of(mockQuiz));
        when(questionRepo.countByQuizId(1)).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> assessmentService.publishQuiz(1));
        verify(quizRepo, never()).save(any());
    }

    @Test
    void publishQuiz_notFound_throwsException() {
        when(quizRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> assessmentService.publishQuiz(99));
    }

    // ==================== DELETE QUIZ ====================

    @Test
    void deleteQuiz_success() {
        when(quizRepo.findById(1)).thenReturn(Optional.of(mockQuiz));
        doNothing().when(questionRepo).deleteByQuizId(1);
        doNothing().when(quizRepo).delete(mockQuiz);

        assessmentService.deleteQuiz(1);

        verify(questionRepo, times(1)).deleteByQuizId(1);
        verify(quizRepo, times(1)).delete(mockQuiz);
    }

    @Test
    void deleteQuiz_notFound_throwsException() {
        when(quizRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> assessmentService.deleteQuiz(99));
        verify(questionRepo, never()).deleteByQuizId(anyInt());
        verify(quizRepo, never()).delete(any());
    }

    // ==================== ADD QUESTION ====================

    // ==================== GET QUESTIONS ====================

    @Test
    void getQuestionsByQuiz_success() {
        when(questionRepo.findByQuizIdOrderByOrderIndex(1))
                .thenReturn(List.of(mockQuestion));

        List<Question> result = assessmentService.getQuestionsByQuiz(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("What is Java?", result.get(0).getText());
    }

    @Test
    void getQuestionsByQuiz_empty() {
        when(questionRepo.findByQuizIdOrderByOrderIndex(99))
                .thenReturn(List.of());

        List<Question> result = assessmentService.getQuestionsByQuiz(99);

        assertTrue(result.isEmpty());
    }

    // ==================== START ATTEMPT ====================

    @Test
    void startAttempt_quizNotFound_throwsException() {
        when(quizRepo.findByQuizId(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> assessmentService.startAttempt(10, 99));
        verify(attemptRepo, never()).save(any());
    }

    // ==================== SUBMIT ATTEMPT ====================

    @Test
    void submitAttempt_allCorrect_passes() {
        when(attemptRepo.findById(500)).thenReturn(Optional.of(mockAttempt));
        when(quizRepo.findByQuizId(1)).thenReturn(Optional.of(mockQuiz));
        when(questionRepo.findByQuizIdOrderByOrderIndex(1))
                .thenReturn(List.of(mockQuestion));
        when(attemptRepo.save(any(Attempt.class)))
                .thenAnswer(i -> i.getArgument(0));

        Attempt result = assessmentService.submitAttempt(500, Map.of(1, "Language"));

        assertNotNull(result.getSubmittedAt());
        assertEquals(100, result.getScore());
        assertTrue(result.isPassed());
    }

    @Test
    void submitAttempt_allWrong_fails() {
        when(attemptRepo.findById(500)).thenReturn(Optional.of(mockAttempt));
        when(quizRepo.findByQuizId(1)).thenReturn(Optional.of(mockQuiz));
        when(questionRepo.findByQuizIdOrderByOrderIndex(1))
                .thenReturn(List.of(mockQuestion));
        when(attemptRepo.save(any(Attempt.class)))
                .thenAnswer(i -> i.getArgument(0));

        Attempt result = assessmentService.submitAttempt(500, Map.of(1, "WrongAnswer"));

        assertEquals(0, result.getScore());
        assertFalse(result.isPassed());
    }

    @Test
    void submitAttempt_emptyAnswers_zeroScore() {
        when(attemptRepo.findById(500)).thenReturn(Optional.of(mockAttempt));
        when(quizRepo.findByQuizId(1)).thenReturn(Optional.of(mockQuiz));
        when(questionRepo.findByQuizIdOrderByOrderIndex(1))
                .thenReturn(List.of(mockQuestion));
        when(attemptRepo.save(any(Attempt.class)))
                .thenAnswer(i -> i.getArgument(0));

        Attempt result = assessmentService.submitAttempt(500, new HashMap<>());

        assertEquals(0, result.getScore());
        assertFalse(result.isPassed());
    }

    @Test
    void submitAttempt_alreadySubmitted_throwsException() {
        mockAttempt.setSubmittedAt(LocalDateTime.now());
        when(attemptRepo.findById(500)).thenReturn(Optional.of(mockAttempt));

        Map<Integer, String> answers = Map.of(1, "Language");
        assertThrows(IllegalStateException.class,
                () -> assessmentService.submitAttempt(500, answers));
        verify(attemptRepo, never()).save(any());
    }

    @Test
    void submitAttempt_notFound_throwsException() {
        when(attemptRepo.findById(999)).thenReturn(Optional.empty());

        Map<Integer, String> emptyMap = Map.of();
        assertThrows(RuntimeException.class,
                () -> assessmentService.submitAttempt(999, emptyMap));
    }

    // ==================== GET ATTEMPTS ====================

    @Test
    void getAttemptsByStudent_success() {
        when(attemptRepo.findByStudentId(10)).thenReturn(List.of(mockAttempt));

        List<Attempt> result = assessmentService.getAttemptsByStudent(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(attemptRepo, times(1)).findByStudentId(10);
    }

    @Test
    void getAttemptsByStudent_empty() {
        when(attemptRepo.findByStudentId(99)).thenReturn(List.of());

        List<Attempt> result = assessmentService.getAttemptsByStudent(99);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAttemptsByQuiz_success() {
        when(attemptRepo.findByQuizId(1)).thenReturn(List.of(mockAttempt));

        List<Attempt> result = assessmentService.getAttemptsByQuiz(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== GET BEST SCORE ====================
    // Interface returns int, not Optional<Attempt>

    @Test
    void getBestScore_noAttempts_returnsZero() {
        when(attemptRepo.findTopByStudentIdAndQuizIdOrderByScoreDesc(10, 99))
                .thenReturn(Optional.empty());

        int result = assessmentService.getBestScore(10, 99);

        assertEquals(0, result);
    }

    @Test
    void createQuiz_blankTitle_throwsException() {
        mockQuiz.setTitle("");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.createQuiz(mockQuiz));
    }

    @Test
    void createQuiz_invalidCourseId_throwsException() {
        mockQuiz.setCourseId(0);
        assertThrows(IllegalArgumentException.class, () -> assessmentService.createQuiz(mockQuiz));
    }

    @Test
    void updateQuiz_success() {
        when(quizRepo.findById(1)).thenReturn(Optional.of(mockQuiz));
        when(quizRepo.save(any(Quiz.class))).thenReturn(mockQuiz);

        Quiz updated = new Quiz();
        updated.setQuizId(1);
        updated.setTitle("Updated Title");

        Quiz result = assessmentService.updateQuiz(updated);
        assertNotNull(result);
        verify(quizRepo).save(any(Quiz.class));
    }

    @Test
    void updateQuiz_notFound_throwsException() {
        when(quizRepo.findById(99)).thenReturn(Optional.empty());
        Quiz updated = new Quiz();
        updated.setQuizId(99);
        assertThrows(IllegalArgumentException.class, () -> assessmentService.updateQuiz(updated));
    }

    @Test
    void addQuestion_success() {
        when(quizRepo.findById(1)).thenReturn(Optional.of(mockQuiz));
        when(questionRepo.countByQuizId(1)).thenReturn(2);
        when(questionRepo.save(any(Question.class))).thenReturn(mockQuestion);

        Question result = assessmentService.addQuestion(1, mockQuestion);
        assertNotNull(result);
        assertEquals(3, mockQuestion.getOrderIndex());
        verify(questionRepo).save(mockQuestion);
    }

    @Test
    void addQuestion_quizNotFound_throwsException() {
        when(quizRepo.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> assessmentService.addQuestion(99, mockQuestion));
    }

    @Test
    void addQuestion_blankText_throwsException() {
        when(quizRepo.findById(1)).thenReturn(Optional.of(mockQuiz));
        mockQuestion.setText("");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.addQuestion(1, mockQuestion));
    }

    @Test
    void addQuestion_blankAnswer_throwsException() {
        when(quizRepo.findById(1)).thenReturn(Optional.of(mockQuiz));
        mockQuestion.setCorrectAnswer(" ");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.addQuestion(1, mockQuestion));
    }

    @Test
    void startAttempt_success_autoPublishes() {
        mockQuiz.setPublished(false);
        when(quizRepo.findByQuizId(1)).thenReturn(Optional.of(mockQuiz));
        when(quizRepo.save(any(Quiz.class))).thenReturn(mockQuiz);
        when(attemptRepo.save(any(Attempt.class))).thenReturn(mockAttempt);

        Attempt result = assessmentService.startAttempt(10, 1);
        assertNotNull(result);
        assertTrue(mockQuiz.isPublished()); // Should be auto-published
        verify(quizRepo).save(mockQuiz);
        verify(attemptRepo).save(any(Attempt.class));
    }

    @Test
    void submitAttempt_quizNotFoundForAttempt_throwsException() {
        when(attemptRepo.findById(500)).thenReturn(Optional.of(mockAttempt));
        when(quizRepo.findByQuizId(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> assessmentService.submitAttempt(500, Map.of()));
    }

    @Test
    void getQuizById_success() {
        when(quizRepo.findById(1)).thenReturn(Optional.of(mockQuiz));
        Quiz result = assessmentService.getQuizById(1);
        assertEquals("Java Quiz", result.getTitle());
    }

    @Test
    void getBestScore_hasAttempts_returnsHighest() {
        mockAttempt.setScore(85);
        when(attemptRepo.findTopByStudentIdAndQuizIdOrderByScoreDesc(10, 1))
                .thenReturn(Optional.of(mockAttempt));

        int result = assessmentService.getBestScore(10, 1);
        assertEquals(85, result);
    }
}