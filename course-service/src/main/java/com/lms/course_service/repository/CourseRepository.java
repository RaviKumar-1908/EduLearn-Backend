package com.lms.course_service.repository;

import com.lms.course_service.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Integer> {

	List<Course> findByTitleContainingIgnoreCase(String title);

	List<Course> findByCategoryIgnoreCase(String category);

	List<Course> findByInstructorId(Integer instructorId);

	List<Course> findByLevelIgnoreCase(String level);

	List<Course> findByIsPublished(Boolean isPublished);

	List<Course> findByCourseIdIn(List<Integer> courseIds);

	List<Course> findByPriceLessThanEqual(Double price);

	@Query("""
			SELECT c FROM Course c
			WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
			OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			OR LOWER(c.category) LIKE LOWER(CONCAT('%', :keyword, '%'))
			OR LOWER(c.level) LIKE LOWER(CONCAT('%', :keyword, '%'))
			OR LOWER(COALESCE(c.language, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			""")
	List<Course> searchByKeyword(@Param("keyword") String keyword);
}
