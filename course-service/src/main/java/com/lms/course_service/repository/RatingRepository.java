package com.lms.course_service.repository;

import com.lms.course_service.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {
    List<Rating> findByCourseId(Integer courseId);
    Optional<Rating> findByCourseIdAndStudentId(Integer courseId, Integer studentId);
    
    // Average rating query
    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.rating) FROM Rating r WHERE r.courseId = :courseId")
    Double getAverageRatingByCourseId(Integer courseId);
    
    long countByCourseId(Integer courseId);
}
