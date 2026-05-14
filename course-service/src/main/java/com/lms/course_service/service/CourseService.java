package com.lms.course_service.service;

import com.lms.course_service.dto.CourseRequestDto;
import com.lms.course_service.dto.CourseResponseDto;

import java.util.List;

public interface CourseService {

	CourseResponseDto createCourse(CourseRequestDto courseRequestDto);

	List<CourseResponseDto> getAllCourses();

	CourseResponseDto getCourseById(int courseId);

	List<CourseResponseDto> getCoursesByIds(List<Integer> courseIds);

	List<CourseResponseDto> getCoursesByCategory(String category);

	List<CourseResponseDto> getCoursesByInstructor(int instructorId);

	List<CourseResponseDto> getCoursesByLevel(String level);

	List<CourseResponseDto> getCoursesByPublishedStatus(boolean isPublished);

	List<CourseResponseDto> getCoursesByPrice(double maxPrice);

	List<CourseResponseDto> searchCourses(String keyword);

	CourseResponseDto updateCourse(int courseId, CourseRequestDto courseRequestDto);

	CourseResponseDto publishCourse(int courseId);

	void deleteCourse(int courseId);

	List<CourseResponseDto> getFeaturedCourses();

	void updateCourseDuration(int courseId, int totalDuration);
	
	// Admin methods
	CourseResponseDto updateCourseStatus(int courseId, String status);
	java.util.Map<String, Object> getAdminStats();
}
