package com.lms.progress.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a student's lesson-level learning activity.
 * Tracks watched duration and completion status per lesson.
 */
@Entity
@Table(
    name = "progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_course_lesson",
        columnNames = {"student_id", "course_id", "lesson_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Progress {

    /** Primary key, auto-incremented. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private int progressId;

    /** ID of the student who owns this progress record. */
    @Column(name = "student_id", nullable = false)
    private int studentId;

    /** ID of the course this lesson belongs to. */
    @Column(name = "course_id", nullable = false)
    private int courseId;

    /** ID of the lesson being tracked. */
    @Column(name = "lesson_id", nullable = false)
    private int lessonId;

    /** Total seconds the student has watched for this lesson. */
    @Column(name = "watched_seconds", nullable = false)
    private int watchedSeconds;

    /** Whether the student has marked (or been auto-marked) complete. */
    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    /** Timestamp of the most recent access to this lesson. */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    /** Timestamp when the lesson was marked complete; null if not yet complete. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
