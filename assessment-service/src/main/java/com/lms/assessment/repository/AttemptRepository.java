package com.lms.assessment.repository;

import com.lms.assessment.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Integer> {

    List<Attempt> findByStudentId(int studentId);

    List<Attempt> findByQuizId(int quizId);

    List<Attempt> findByStudentIdAndQuizId(int studentId, int quizId);

    int countByStudentIdAndQuizId(int studentId, int quizId);
    
    int countByStudentIdAndQuizIdAndSubmittedAtIsNotNull(int studentId, int quizId);

    @Query("SELECT a FROM Attempt a WHERE a.studentId = :studentId AND a.quizId = :quizId ORDER BY a.score DESC")
    Optional<Attempt> findTopByStudentIdAndQuizIdOrderByScoreDesc(
            @Param("studentId") int studentId,
            @Param("quizId") int quizId);
}
