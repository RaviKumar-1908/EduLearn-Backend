package com.lms.lesson_service.repository;

import com.lms.lesson_service.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {

	List<Lesson> findByCourseIdOrderByOrderIndexAsc(Integer courseId);

	List<Lesson> findByCourseIdAndPreviewTrueOrderByOrderIndexAsc(Integer courseId);

	List<Lesson> findByCourseIdAndPublishedTrueOrderByOrderIndexAsc(Integer courseId);
}
