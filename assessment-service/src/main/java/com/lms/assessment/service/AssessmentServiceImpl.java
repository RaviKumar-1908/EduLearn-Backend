package com.lms.assessment.service;

import com.lms.assessment.entity.Attempt;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.Quiz;
import com.lms.assessment.repository.AttemptRepository;
import com.lms.assessment.repository.QuestionRepository;
import com.lms.assessment.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AssessmentServiceImpl implements AssessmentService {

    @Autowired
    private QuizRepository quizRepo;

    @Autowired
    private QuestionRepository questionRepo;

    @Autowired
    private AttemptRepository attemptRepo;

    public AssessmentServiceImpl() {}

    // ─── Quiz Operations ──────────────────────────────────────────────────────

    @Override
    public Quiz createQuiz(Quiz quiz) {
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            throw new IllegalArgumentException("Quiz title must not be blank");
        }
        if (quiz.getCourseId() <= 0) {
            throw new IllegalArgumentException("Quiz must be linked to a valid courseId");
        }
        // Allow the quiz to be published on creation if the field is set
        return quizRepo.save(quiz);
    }

    @Override
    public Quiz updateQuiz(Quiz quiz) {
        Quiz existing = quizRepo.findById(quiz.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quiz.getQuizId()));
        existing.setTitle(quiz.getTitle());
        existing.setDescription(quiz.getDescription());
        existing.setTimeLimitMinutes(quiz.getTimeLimitMinutes());
        existing.setPassingScore(quiz.getPassingScore());
        existing.setPublished(quiz.isPublished());
        return quizRepo.save(existing);
    }

    @Override
    @Transactional
    public void deleteQuiz(int quizId) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));
        questionRepo.deleteByQuizId(quizId);
        quizRepo.delete(quiz);
    }

    @Override
    public void publishQuiz(int quizId) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));
        int questionCount = questionRepo.countByQuizId(quizId);
        if (questionCount == 0) {
            throw new IllegalStateException("Cannot publish a quiz with no questions");
        }
        quiz.setPublished(true);
        quizRepo.save(quiz);
    }

    // ─── Question Operations ──────────────────────────────────────────────────

    @Override
    public Question addQuestion(int quizId, Question question) {
        quizRepo.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with ID: " + quizId));
        if (question.getText() == null || question.getText().isBlank()) {
            throw new IllegalArgumentException("Question text must not be blank");
        }
        if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
            throw new IllegalArgumentException("Correct answer must not be blank");
        }
        question.setQuizId(quizId);
        int currentCount = questionRepo.countByQuizId(quizId);
        question.setOrderIndex(currentCount + 1);
        return questionRepo.save(question);
    }

    // ─── Attempt Operations ───────────────────────────────────────────────────

    @Override
    public Attempt startAttempt(int studentId, int quizId) {
        Quiz quiz = quizRepo.findByQuizId(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        // Access is controlled via enrollment and lesson publication at the gateway/frontend level.
        // We remove the internal isPublished check to support the new unified publishing model.
        
        // Force publish if not already (safeguard)
        if (!quiz.isPublished()) {
            quiz.setPublished(true);
            quizRepo.save(quiz);
        }

        Attempt attempt = new Attempt(quizId, studentId);
        return attemptRepo.save(attempt);
    }

    @Override
    @Transactional
    public Attempt submitAttempt(int attemptId, Map<Integer, String> answers) {
        Attempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        if (attempt.getSubmittedAt() != null) {
            throw new IllegalStateException("This attempt has already been submitted");
        }

        Quiz quiz = quizRepo.findByQuizId(attempt.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found for attempt"));

        List<Question> questions = questionRepo.findByQuizIdOrderByOrderIndex(attempt.getQuizId());

        // Auto-grade: calculate total marks earned
        int totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
        int earnedMarks = 0;

        for (Question q : questions) {
            String studentAnswer = answers.get(q.getQuestionId());
            if (studentAnswer != null &&
                    studentAnswer.trim().equalsIgnoreCase(q.getCorrectAnswer().trim())) {
                earnedMarks += q.getMarks();
            }
        }

        // Score as percentage (0–100)
        int scorePercent = (totalMarks > 0) ? (int) Math.round((earnedMarks * 100.0) / totalMarks) : 0;

        attempt.setAnswers(answers);
        attempt.setScore(scorePercent);
        attempt.setPassed(scorePercent >= quiz.getPassingScore());
        attempt.setSubmittedAt(LocalDateTime.now());

        return attemptRepo.save(attempt);
    }

    // ─── Query Operations ─────────────────────────────────────────────────────

    @Override
    public List<Quiz> getQuizzesByCourse(int courseId) {
        return quizRepo.findByCourseId(courseId);
    }

    @Override
    public Quiz getQuizById(int quizId) {
        return quizRepo.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with ID: " + quizId));
    }

    @Override
    public List<Question> getQuestionsByQuiz(int quizId) {
        return questionRepo.findByQuizIdOrderByOrderIndex(quizId);
    }

    @Override
    public List<Attempt> getAttemptsByStudent(int studentId) {
        return attemptRepo.findByStudentId(studentId);
    }

    @Override
    public List<Attempt> getAttemptsByQuiz(int quizId) {
        return attemptRepo.findByQuizId(quizId);
    }

    @Override
    public int getBestScore(int studentId, int quizId) {
        return attemptRepo
                .findTopByStudentIdAndQuizIdOrderByScoreDesc(studentId, quizId)
                .map(Attempt::getScore)
                .orElse(0);
    }
}
