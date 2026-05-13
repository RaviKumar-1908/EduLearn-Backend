package com.lms.assessment.resource;

import com.lms.assessment.entity.Attempt;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.Quiz;
import com.lms.assessment.service.AssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/assessment", "/assessment"})
public class AssessmentResource {

    private static final Logger log = LoggerFactory.getLogger(AssessmentResource.class);

    private static final String MSG = "message";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String BACKEND_ERROR = "BACKEND ERROR: ";
    private static final String QUIZ_NOT_FOUND = "Quiz not found";
    private static final String INVALID_STATE = "Invalid state";

    @Autowired
    private AssessmentService assessService;

    public AssessmentResource() {}

    // ─── Quiz Endpoints (/quizzes) ────────────────────────────────────────────

    // POST /quizzes
    @PostMapping("/quizzes")
    public ResponseEntity<?> createQuiz(@RequestBody Quiz quiz) {
        try {
            Quiz created = assessService.createQuiz(quiz);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : "Invalid request"));
        } catch (Exception e) {
            log.error("Error creating quiz", e);
            return ResponseEntity.status(500).body(Map.of(MSG, BACKEND_ERROR + (e.getMessage() != null ? e.getMessage() : INTERNAL_SERVER_ERROR)));
        }
    }

    // PUT /quizzes
    @PutMapping("/quizzes")
    public ResponseEntity<?> updateQuiz(@RequestBody Quiz quiz) {
        try {
            Quiz updated = assessService.updateQuiz(quiz);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : QUIZ_NOT_FOUND));
        } catch (Exception e) {
            log.error("Error updating quiz", e);
            return ResponseEntity.status(500).body(Map.of(MSG, BACKEND_ERROR + (e.getMessage() != null ? e.getMessage() : INTERNAL_SERVER_ERROR)));
        }
    }

    // DELETE /quizzes/{quizId}
    @DeleteMapping("/quizzes/{quizId}")
    public ResponseEntity<?> deleteQuiz(@PathVariable int quizId) {
        try {
            assessService.deleteQuiz(quizId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : QUIZ_NOT_FOUND));
        } catch (Exception e) {
            log.error("Error deleting quiz", e);
            return ResponseEntity.status(500).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : INTERNAL_SERVER_ERROR));
        }
    }

    // PUT /quizzes/{quizId}/publish
    @PutMapping("/quizzes/{quizId}/publish")
    public ResponseEntity<?> publishQuiz(@PathVariable int quizId) {
        try {
            assessService.publishQuiz(quizId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : QUIZ_NOT_FOUND));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : INVALID_STATE));
        } catch (Exception e) {
            log.error("Error publishing quiz", e);
            return ResponseEntity.status(500).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : INTERNAL_SERVER_ERROR));
        }
    }

    // POST /quizzes/{quizId}/questions
    @PostMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<?> addQuestion(
            @PathVariable int quizId,
            @RequestBody Question question) {
        try {
            Question saved = assessService.addQuestion(quizId, question);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : "Invalid question"));
        } catch (Exception e) {
            log.error("Error adding question", e);
            return ResponseEntity.status(500).body(Map.of(MSG, BACKEND_ERROR + (e.getMessage() != null ? e.getMessage() : INTERNAL_SERVER_ERROR)));
        }
    }

    // GET /quizzes/{quizId}/questions
    @GetMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<List<Question>> getQuestionsByQuiz(@PathVariable int quizId) {
        List<Question> questions = assessService.getQuestionsByQuiz(quizId);
        return ResponseEntity.ok(questions);
    }

    // GET /quizzes/{quizId}
    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<?> getQuizById(@PathVariable int quizId) {
        try {
            Quiz quiz = assessService.getQuizById(quizId);
            return ResponseEntity.ok(quiz);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : QUIZ_NOT_FOUND));
        } catch (Exception e) {
            log.error("Error getting quiz", e);
            return ResponseEntity.status(500).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : INTERNAL_SERVER_ERROR));
        }
    }

    // GET /quizzes/course/{courseId}
    @GetMapping("/quizzes/course/{courseId}")
    public ResponseEntity<List<Quiz>> getByCourse(@PathVariable int courseId) {
        List<Quiz> quizzes = assessService.getQuizzesByCourse(courseId);
        return ResponseEntity.ok(quizzes);
    }

    // ─── Attempt Endpoints (/attempts) ───────────────────────────────────────

    // POST /attempts/start?studentId=1&quizId=2
    @PostMapping("/attempts/start")
    public ResponseEntity<?> startAttempt(
            @RequestParam int studentId,
            @RequestParam int quizId) {
        try {
            Attempt attempt = assessService.startAttempt(studentId, quizId);
            return ResponseEntity.status(HttpStatus.CREATED).body(attempt);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : QUIZ_NOT_FOUND));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : INVALID_STATE));
        } catch (Exception e) {
            log.error("Error starting attempt", e);
            return ResponseEntity.status(500).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : INTERNAL_SERVER_ERROR));
        }
    }

    // POST /attempts/{attemptId}/submit
    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<?> submitAttempt(
            @PathVariable int attemptId,
            @RequestBody Map<Integer, String> answers) {
        try {
            Attempt result = assessService.submitAttempt(attemptId, answers);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : "Attempt not found"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : INVALID_STATE));
        } catch (Exception e) {
            log.error("Error submitting attempt", e);
            return ResponseEntity.status(500).body(Map.of(MSG, e.getMessage() != null ? e.getMessage() : INTERNAL_SERVER_ERROR));
        }
    }

    // GET /attempts/student/{studentId}
    @GetMapping("/attempts/student/{studentId}")
    public ResponseEntity<List<Attempt>> getByStudent(@PathVariable int studentId) {
        List<Attempt> attempts = assessService.getAttemptsByStudent(studentId);
        return ResponseEntity.ok(attempts);
    }

    // GET /attempts/quiz/{quizId}
    @GetMapping("/attempts/quiz/{quizId}")
    public ResponseEntity<List<Attempt>> getByQuiz(@PathVariable int quizId) {
        List<Attempt> attempts = assessService.getAttemptsByQuiz(quizId);
        return ResponseEntity.ok(attempts);
    }

    // GET /attempts/best?studentId=1&quizId=2
    @GetMapping("/attempts/best")
    public ResponseEntity<Integer> getBestScore(
            @RequestParam int studentId,
            @RequestParam int quizId) {
        int best = assessService.getBestScore(studentId, quizId);
        return ResponseEntity.ok(best);
    }
}
