package com.lms.assessment.repository;

import com.lms.assessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    List<Question> findByQuizIdOrderByOrderIndex(int quizId);

    int countByQuizId(int quizId);

    void deleteByQuizId(int quizId);
}
