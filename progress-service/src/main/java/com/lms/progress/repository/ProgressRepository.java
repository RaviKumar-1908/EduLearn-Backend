package com.lms.progress.repository;

import com.lms.progress.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Progress} entities.
 * Provides derived-query and JPQL methods aligned with the class diagram.
 */
@Repository
public interface ProgressRepository extends JpaRepository<Progress, Integer> {

    /**
     * Returns all progress records for the given student within a course.
     * Used to calculate course-level completion percentage.
     */
    List<Progress> findByStudentIdAndCourseId(int studentId, int courseId);

    /**
     * Returns the progress record for a specific student/lesson combination.
     * Used by getLessonProgress() to fetch per-lesson state.
     */
    Optional<Progress> findByStudentIdAndLessonId(int studentId, int lessonId);

    /**
     * Returns every progress record owned by a student across all courses.
     */
    List<Progress> findByStudentId(int studentId);

    /**
     * Returns the exact progress record for (student, course, lesson).
     * Used internally by trackProgress() and markLessonComplete() to
     * decide whether to INSERT or UPDATE.
     */
    Optional<Progress> findByStudentIdAndCourseIdAndLessonId(int studentId, int courseId, int lessonId);

    /**
     * Counts the number of lessons the student has completed inside a course.
     * Used by getCourseProgress() to calculate the completion percentage.
     *
     * @return number of rows where isCompleted = true
     */
    @Query("SELECT COUNT(p) FROM Progress p " +
           "WHERE p.studentId = :studentId " +
           "AND p.courseId = :courseId " +
           "AND p.isCompleted = true")
    long countCompletedByStudentIdAndCourseId(
            @Param("studentId") int studentId,
            @Param("courseId")  int courseId);
}
