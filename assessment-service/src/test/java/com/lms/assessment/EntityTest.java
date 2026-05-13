package com.lms.assessment;

import com.lms.assessment.entity.Attempt;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.Quiz;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    // =========================================================================
    // Quiz Entity
    // =========================================================================

    @Test
    @DisplayName("Quiz default constructor should set safe defaults")
    void quiz_DefaultConstructor_SetsDefaults() {
        Quiz quiz = new Quiz();

        assertFalse(quiz.isPublished());
        assertEquals(3, quiz.getMaxAttempts());
        assertEquals(30, quiz.getTimeLimitMinutes());
        assertEquals(60, quiz.getPassingScore());
    }

    @Test
    @DisplayName("Quiz setters and getters should work correctly")
    void quiz_SettersGetters() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1);
        quiz.setCourseId(10);
        quiz.setTitle("Java Basics");
        quiz.setDescription("Test your Java knowledge");
        quiz.setTimeLimitMinutes(45);
        quiz.setPassingScore(70);
        quiz.setMaxAttempts(5);
        quiz.setPublished(true);

        assertEquals(1, quiz.getQuizId());
        assertEquals(10, quiz.getCourseId());
        assertEquals("Java Basics", quiz.getTitle());
        assertEquals("Test your Java knowledge", quiz.getDescription());
        assertEquals(45, quiz.getTimeLimitMinutes());
        assertEquals(70, quiz.getPassingScore());
        assertEquals(5, quiz.getMaxAttempts());
        assertTrue(quiz.isPublished());
    }

    @Test
    @DisplayName("Quiz toString should include key fields")
    void quiz_ToString_ContainsKeyFields() {
        Quiz quiz = new Quiz();
        quiz.setQuizId(1);
        quiz.setTitle("Test");
        quiz.setCourseId(5);

        String str = quiz.toString();

        assertTrue(str.contains("quizId=1"));
        assertTrue(str.contains("Test"));
        assertTrue(str.contains("courseId=5"));
    }

    // =========================================================================
    // Question Entity
    // =========================================================================

    @Test
    @DisplayName("Question default constructor should set safe defaults")
    void question_DefaultConstructor_SetsDefaults() {
        Question q = new Question();

        assertEquals(1, q.getMarks());
        assertEquals(0, q.getOrderIndex());
    }

    @Test
    @DisplayName("Question setters and getters should work correctly")
    void question_SettersGetters() {
        Question q = new Question();
        q.setQuestionId(1);
        q.setQuizId(2);
        q.setText("What is polymorphism?");
        q.setType("MCQ");
        q.setOptions(List.of("A", "B", "C", "D"));
        q.setCorrectAnswer("A");
        q.setMarks(5);
        q.setOrderIndex(3);

        assertEquals(1, q.getQuestionId());
        assertEquals(2, q.getQuizId());
        assertEquals("What is polymorphism?", q.getText());
        assertEquals("MCQ", q.getType());
        assertEquals(4, q.getOptions().size());
        assertEquals("A", q.getCorrectAnswer());
        assertEquals(5, q.getMarks());
        assertEquals(3, q.getOrderIndex());
    }

    @Test
    @DisplayName("Question toString should include key fields")
    void question_ToString_ContainsKeyFields() {
        Question q = new Question();
        q.setQuestionId(7);
        q.setType("TrueFalse");

        String str = q.toString();

        assertTrue(str.contains("questionId=7"));
        assertTrue(str.contains("TrueFalse"));
    }

    // =========================================================================
    // Attempt Entity
    // =========================================================================

    @Test
    @DisplayName("Attempt default constructor should set startedAt")
    void attempt_DefaultConstructor_SetsStartedAt() {
        Attempt a = new Attempt();

        assertNotNull(a.getStartedAt());
        assertNull(a.getSubmittedAt());
        assertFalse(a.isPassed());
        assertEquals(0, a.getScore());
    }

    @Test
    @DisplayName("Attempt parameterized constructor should bind quizId and studentId")
    void attempt_ParameterizedConstructor() {
        Attempt a = new Attempt(5, 42);

        assertEquals(5, a.getQuizId());
        assertEquals(42, a.getStudentId());
        assertNotNull(a.getStartedAt());
    }

    @Test
    @DisplayName("Attempt setters and getters should work correctly")
    void attempt_SettersGetters() {
        Attempt a = new Attempt();
        a.setAttemptId(10);
        a.setQuizId(1);
        a.setStudentId(5);
        a.setScore(80);
        a.setPassed(true);

        assertEquals(10, a.getAttemptId());
        assertEquals(1, a.getQuizId());
        assertEquals(5, a.getStudentId());
        assertEquals(80, a.getScore());
        assertTrue(a.isPassed());
    }

    @Test
    @DisplayName("Attempt toString should include key fields")
    void attempt_ToString_ContainsKeyFields() {
        Attempt a = new Attempt(3, 7);
        a.setAttemptId(1);
        a.setScore(90);

        String str = a.toString();

        assertTrue(str.contains("attemptId=1"));
        assertTrue(str.contains("score=90"));
    }
}
