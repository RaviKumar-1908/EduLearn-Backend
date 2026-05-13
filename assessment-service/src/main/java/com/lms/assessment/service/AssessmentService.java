package com.lms.assessment.service;

import com.lms.assessment.entity.Attempt;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.Quiz;

import java.util.List;
import java.util.Map;

public interface AssessmentService {

    Quiz createQuiz(Quiz quiz);

    Quiz updateQuiz(Quiz quiz);

    void deleteQuiz(int quizId);

    void publishQuiz(int quizId);

    Question addQuestion(int quizId, Question question);

    Attempt startAttempt(int studentId, int quizId);

    Attempt submitAttempt(int attemptId, Map<Integer, String> answers);

    List<Quiz> getQuizzesByCourse(int courseId);
    Quiz getQuizById(int quizId);
    List<Question> getQuestionsByQuiz(int quizId);

    List<Attempt> getAttemptsByStudent(int studentId);

    List<Attempt> getAttemptsByQuiz(int quizId);

    int getBestScore(int studentId, int quizId);
}
