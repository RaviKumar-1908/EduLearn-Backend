package com.lms.progress.service;

import com.lms.progress.entity.Certificate;
import com.lms.progress.entity.Progress;

import java.util.List;
import java.util.Optional;

/**
 * Contract for all progress-tracking and certificate operations.
 * Implemented by {@link ProgressServiceImpl}.
 */
public interface ProgressService {

    /**
     * Records or updates the number of seconds a student has watched
     * for a specific lesson. Creates a new row on first access;
     * accumulates watchedSeconds on subsequent calls.
     *
     * @param studentId      ID of the student
     * @param courseId       ID of the parent course
     * @param lessonId       ID of the lesson being watched
     * @param watchedSeconds seconds watched in this session
     */
    void trackProgress(int studentId, int courseId, int lessonId, int watchedSeconds);

    /**
     * Marks a specific lesson as completed for the student.
     * Sets isCompleted = true and records completedAt timestamp.
     *
     * @param studentId ID of the student
     * @param courseId  ID of the parent course
     * @param lessonId  ID of the lesson to mark complete
     */
    void markLessonComplete(int studentId, int courseId, int lessonId);

    /**
     * Calculates and returns the course-level completion percentage
     * as an integer in the range [0, 100].
     * Formula: (completed lessons / total tracked lessons) * 100
     *
     * @param studentId ID of the student
     * @param courseId  ID of the course
     * @return completion percentage
     */
    int getCourseProgress(int studentId, int courseId);

    /**
     * Returns the progress record for a specific student / lesson pair.
     *
     * @param studentId ID of the student
     * @param lessonId  ID of the lesson
     * @return Optional containing the progress record, or empty if none found
     */
    Optional<Progress> getLessonProgress(int studentId, int lessonId);

    /**
     * Issues a certificate when the student has reached 100% progress
     * on a course. Throws exceptions if the course is incomplete or
     * a certificate has already been issued.
     *
     * @param studentId      ID of the student
     * @param courseId       ID of the course
     * @param courseName     Name of the course
     * @param instructorName Name of the instructor
     * @param courseLevel    Level of the course
     * @param courseDuration Total duration in hours
     * @return the newly created {@link Certificate}
     */
    Certificate issueCertificate(int studentId, int courseId, String courseName, String instructorName, String courseLevel, Integer courseDuration);

    /**
     * Retrieves an existing certificate for the student / course pair.
     *
     * @param studentId ID of the student
     * @param courseId  ID of the course
     * @return Optional containing the certificate, or empty if not yet issued
     */
    Optional<Certificate> getCertificate(int studentId, int courseId);

    /**
     * Verifies a certificate by its unique verification code.
     * Intended for public, unauthenticated use by third parties.
     *
     * @param verificationCode UUID-based code printed on the certificate
     * @return the matching {@link Certificate}
     */
    Certificate verifyCertificate(String verificationCode);

    /**
     * Returns all progress records for a student across all courses.
     *
     * @param studentId ID of the student
     * @return list of progress records
     */
    List<Progress> getAllProgressByStudent(int studentId);

    /**
     * Returns all certificates earned by a student across all courses.
     *
     * @param studentId ID of the student
     * @return list of certificates
     */
    List<Certificate> getCertificatesByStudent(int studentId);
}
