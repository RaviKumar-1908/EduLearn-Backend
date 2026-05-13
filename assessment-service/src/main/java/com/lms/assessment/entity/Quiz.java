package com.lms.assessment.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int quizId;

    @Column(nullable = false)
    private int courseId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private int timeLimitMinutes;

    @Column(nullable = false)
    private int passingScore;


    @com.fasterxml.jackson.annotation.JsonProperty("isPublished")
    @Column(name = "is_published", nullable = false)
    private boolean isPublished;

    private int maxAttempts;

    public Quiz() {
        this.isPublished = false;
        this.timeLimitMinutes = 30;
        this.passingScore = 60;
        this.maxAttempts = 3;
    }

    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(int timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }


    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean isPublished) { this.isPublished = isPublished; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    @Override
    public String toString() {
        return "Quiz{" +
                "quizId=" + quizId +
                ", courseId=" + courseId +
                ", title='" + title + '\'' +
                ", passingScore=" + passingScore +
                ", published=" + isPublished +
                '}';
    }
}
