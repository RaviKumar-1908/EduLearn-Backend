package com.lms.assessment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "attempts")
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int attemptId;

    @Column(nullable = false)
    private int quizId;

    @Column(nullable = false)
    private int studentId;

    private int score;

    private boolean passed;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    // key = questionId, value = student's answer
    @ElementCollection
    @CollectionTable(name = "attempt_answers", joinColumns = @JoinColumn(name = "attempt_id"))
    @MapKeyColumn(name = "question_id")
    @Column(name = "answer")
    private Map<Integer, String> answers = new HashMap<>();

    public Attempt() {
        this.startedAt = LocalDateTime.now();
    }

    public Attempt(int quizId, int studentId) {
        this();
        this.quizId = quizId;
        this.studentId = studentId;
    }

    public int getAttemptId() { return attemptId; }
    public void setAttemptId(int attemptId) { this.attemptId = attemptId; }

    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public Map<Integer, String> getAnswers() { return answers; }
    public void setAnswers(Map<Integer, String> answers) { this.answers = answers; }

    @Override
    public String toString() {
        return "Attempt{" +
                "attemptId=" + attemptId +
                ", quizId=" + quizId +
                ", studentId=" + studentId +
                ", score=" + score +
                ", passed=" + passed +
                ", startedAt=" + startedAt +
                ", submittedAt=" + submittedAt +
                '}';
    }
}
