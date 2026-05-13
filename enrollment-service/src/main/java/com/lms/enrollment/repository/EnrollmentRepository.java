package com.lms.enrollment.repository;

import com.lms.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    List<Enrollment> findByStudentId(int studentId);

    List<Enrollment> findByCourseId(int courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(int studentId, int courseId);

    boolean existsByStudentIdAndCourseId(int studentId, int courseId);

    @Query("SELECT e FROM Enrollment e WHERE e.studentId = :studentId AND e.courseId = :courseId AND e.status <> 'Cancelled'")
    Optional<Enrollment> findActiveByStudentIdAndCourseId(@Param("studentId") int studentId, @Param("courseId") int courseId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Enrollment e WHERE e.studentId = :studentId AND e.courseId = :courseId AND e.status <> 'Cancelled'")
    boolean existsActiveByStudentIdAndCourseId(@Param("studentId") int studentId, @Param("courseId") int courseId);

    List<Enrollment> findByStatus(String status);
    int countByStatus(String status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId AND e.status <> 'Cancelled'")
    int countActiveByCourseId(@Param("courseId") int courseId);

    int countByCourseId(int courseId);

    @Query("SELECT e FROM Enrollment e WHERE e.studentId = :studentId AND e.status = 'Completed'")
    List<Enrollment> findCompletedByStudentId(@Param("studentId") int studentId);

    void deleteByCourseId(int courseId);
}
