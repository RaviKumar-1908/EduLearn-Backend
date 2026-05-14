package com.lms.course_service.service.impl;

import com.lms.course_service.dto.CourseRequestDto;
import com.lms.course_service.dto.CourseResponseDto;
import com.lms.course_service.entity.Course;
import com.lms.course_service.mapper.CourseMapper;
import com.lms.course_service.repository.CourseRepository;
import com.lms.course_service.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

	private final CourseRepository courseRepository;
	private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

	@Override
	public CourseResponseDto createCourse(CourseRequestDto courseRequestDto) {
		log.info("Creating new course with title: {}", courseRequestDto.getTitle());

		Course course = CourseMapper.mapToCourse(courseRequestDto);
		course.setCourseId(null);
		course.setCreatedAt(LocalDateTime.now());
		course.setIsPublished(false);

		Course savedCourse = courseRepository.save(course);
		log.info("Course created successfully with ID: {}", savedCourse.getCourseId());

		java.util.Map<String, Object> createdEvent = new java.util.HashMap<>();
		createdEvent.put("userId", savedCourse.getInstructorId());
		createdEvent.put("courseId", savedCourse.getCourseId());
		createdEvent.put("title", "Course Draft Created");
		createdEvent.put("message", "Your course '" + savedCourse.getTitle() + "' has been created successfully.");
		createdEvent.put("type", "COURSE_CREATED");
		createdEvent.put("relatedEntityId", savedCourse.getCourseId());
		createdEvent.put("relatedEntityType", "COURSE");
		// Robust event publishing
		try {
			log.info("[RabbitMQ] ▶ Publishing COURSE_CREATED event | userId={}", savedCourse.getInstructorId());
			rabbitTemplate.convertAndSend("lms.events.exchange", "notification.course.created", createdEvent);
			
			// Notify Admin about new course pending approval
			java.util.Map<String, Object> adminEvent = new java.util.HashMap<>();
			adminEvent.put("userId", 1); // Assuming 1 is a primary admin or broadcast target
			adminEvent.put("title", "New Course Pending! 📝");
			adminEvent.put("message", "Instructor " + savedCourse.getInstructorId() + " has created a new course: " + savedCourse.getTitle());
			adminEvent.put("type", "ADMIN_ALERT");
			adminEvent.put("relatedEntityId", savedCourse.getCourseId());
			adminEvent.put("relatedEntityType", "COURSE");
			rabbitTemplate.convertAndSend("lms.events.exchange", "notification.auth.admin.alert", adminEvent);
			
			log.info("[RabbitMQ] ✔ Events published successfully");
		} catch (Exception e) {
			log.error("[RabbitMQ] ✘ Failed to publish creation events: {}", e.getMessage());
		}

		return CourseMapper.mapToCourseResponseDto(savedCourse);
	}

	public List<CourseResponseDto> getAllCourses() {
		log.info("Fetching all courses");
		return courseRepository.findAll().stream()
				.map(CourseMapper::mapToCourseResponseDto)
				.collect(Collectors.toList());
	}

	public CourseResponseDto getCourseById(int courseId) {
		log.info("Fetching course by ID: {}", courseId);
		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
		return CourseMapper.mapToCourseResponseDto(course);
	}

	@Override
	public List<CourseResponseDto> getCoursesByIds(List<Integer> courseIds) {
		log.info("Fetching {} courses in bulk", courseIds.size());
		return courseRepository.findByCourseIdIn(courseIds).stream()
				.map(CourseMapper::mapToCourseResponseDto)
				.collect(Collectors.toList());
	}

	public List<CourseResponseDto> getCoursesByCategory(String category) {
		log.info("Fetching courses by category: {}", category);
		return courseRepository.findByCategoryIgnoreCase(category).stream()
				.map(CourseMapper::mapToCourseResponseDto)
				.collect(Collectors.toList());
	}

	public List<CourseResponseDto> getCoursesByInstructor(int instructorId) {
		log.info("Fetching courses by instructor ID: {}", instructorId);
		return courseRepository.findByInstructorId(instructorId).stream()
				.map(CourseMapper::mapToCourseResponseDto)
				.collect(Collectors.toList());
	}

	public List<CourseResponseDto> getCoursesByLevel(String level) {
		log.info("Fetching courses by level: {}", level);
		return courseRepository.findByLevelIgnoreCase(level).stream()
				.map(CourseMapper::mapToCourseResponseDto)
				.collect(Collectors.toList());
	}

	public List<CourseResponseDto> getCoursesByPublishedStatus(boolean isPublished) {
		log.info("Fetching courses by published status: {}", isPublished);
		return courseRepository.findByIsPublished(isPublished).stream()
				.map(CourseMapper::mapToCourseResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public List<CourseResponseDto> getCoursesByPrice(double maxPrice) {
		log.info("Fetching courses with price less than or equal to: {}", maxPrice);
		return courseRepository.findByPriceLessThanEqual(maxPrice).stream()
				.map(CourseMapper::mapToCourseResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public List<CourseResponseDto> searchCourses(String keyword) {
		log.info("Searching courses with keyword: {}", keyword);
		List<Course> courses;
		if (keyword == null || keyword.isBlank()) {
			courses = courseRepository.findAll();
		} else {
			courses = courseRepository.searchByKeyword(keyword.trim());
		}
		return courses.stream()
				.map(CourseMapper::mapToCourseResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public CourseResponseDto updateCourse(int courseId, CourseRequestDto courseRequestDto) {
		log.info("Updating course with ID: {}", courseId);

		Course existingCourse = courseRepository.findById(courseId)
				.orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));

		existingCourse.setTitle(courseRequestDto.getTitle());
		existingCourse.setDescription(courseRequestDto.getDescription());
		existingCourse.setCategory(courseRequestDto.getCategory());
		existingCourse.setLevel(courseRequestDto.getLevel());
		existingCourse.setPrice(courseRequestDto.getPrice());
		existingCourse.setInstructorId(courseRequestDto.getInstructorId());
		existingCourse.setThumbnailUrl(courseRequestDto.getThumbnailUrl());
		existingCourse.setTotalDuration(courseRequestDto.getTotalDuration());
		existingCourse.setLanguage(courseRequestDto.getLanguage());

		Course updatedCourse = courseRepository.save(existingCourse);
		log.info("Course updated successfully with ID: {}", updatedCourse.getCourseId());
		return CourseMapper.mapToCourseResponseDto(updatedCourse);
	}

	@Override
	public CourseResponseDto publishCourse(int courseId) {
		log.info("Publishing course with ID: {}", courseId);

		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
		course.setIsPublished(true);

		Course publishedCourse = courseRepository.save(course);
		log.info("Course published successfully with ID: {}", publishedCourse.getCourseId());

		// Notify about publication
		java.util.Map<String, Object> event = new java.util.HashMap<>();
		event.put("userId", publishedCourse.getInstructorId());
		event.put("instructorId", publishedCourse.getInstructorId());
		event.put("courseId", publishedCourse.getCourseId());
		event.put("title", "New Course Published!");
		event.put("message", "A new course '" + publishedCourse.getTitle() + "' is now available.");
		event.put("type", "COURSE_PUBLISHED");
		event.put("relatedEntityId", publishedCourse.getCourseId());
		event.put("relatedEntityType", "COURSE");
		// Robust event publishing
		try {
			log.info("[RabbitMQ] ▶ Publishing COURSE_PUBLISHED event | courseId={}", publishedCourse.getCourseId());
			rabbitTemplate.convertAndSend("lms.events.exchange", "notification.course.published", event);
			log.info("[RabbitMQ] ✔ Event published successfully");
		} catch (Exception e) {
			log.error("[RabbitMQ] ✘ Failed to publish COURSE_PUBLISHED event: {}", e.getMessage());
		}

		return CourseMapper.mapToCourseResponseDto(publishedCourse);
	}

	@Override
	public void deleteCourse(int courseId) {
		log.info("Deleting course with ID: {}", courseId);

		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
		courseRepository.delete(course);
		log.info("Course deleted successfully with ID: {}", courseId);

		// Notify other services to clean up
		java.util.Map<String, Object> deleteEvent = new java.util.HashMap<>();
		deleteEvent.put("userId", course.getInstructorId());
		deleteEvent.put("instructorId", course.getInstructorId());
		deleteEvent.put("courseId", courseId);
		deleteEvent.put("title", "Course Deleted");
		deleteEvent.put("message", "Course '" + course.getTitle() + "' has been removed.");
		deleteEvent.put("type", "COURSE_DELETED");
		// Robust event publishing
		try {
			log.info("[RabbitMQ] ▶ Publishing COURSE_DELETED event | courseId={}", courseId);
			rabbitTemplate.convertAndSend("lms.events.exchange", "notification.course.deleted", deleteEvent);
			log.info("[RabbitMQ] ✔ Event published successfully");
		} catch (Exception e) {
			log.error("[RabbitMQ] ✘ Failed to publish COURSE_DELETED event: {}", e.getMessage());
		}
	}

	public List<CourseResponseDto> getFeaturedCourses() {
		log.info("Fetching featured published courses");
		return courseRepository.findByIsPublished(true).stream()
				.sorted(Comparator.comparing(Course::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(6)
				.map(CourseMapper::mapToCourseResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public void updateCourseDuration(int courseId, int totalDuration) {
		log.info("Updating duration for course ID: {} to {} minutes", courseId, totalDuration);
		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
		course.setTotalDuration(totalDuration);
		courseRepository.save(course);
	}

	@Override
	public CourseResponseDto updateCourseStatus(int courseId, String status) {
		log.info("Updating status for course ID: {} to {}", courseId, status);
		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));

		course.setStatus(status.toUpperCase());

		// If approved, we might want to do something else, but for now just update
		// status
		if ("APPROVED".equalsIgnoreCase(status)) {
			// maybe auto-publish? No, let instructor decide.
		}

		Course saved = courseRepository.save(course);
		return CourseMapper.mapToCourseResponseDto(saved);
	}

	@Override
	public java.util.Map<String, Object> getAdminStats() {
		log.info("Fetching admin statistics for course service");
		java.util.Map<String, Object> stats = new java.util.HashMap<>();

		long totalCourses = courseRepository.count();
		long publishedCourses = courseRepository.findByIsPublished(true).size();
		long pendingCourses = courseRepository.findAll().stream().filter(c -> "PENDING".equalsIgnoreCase(c.getStatus()))
				.count();

		stats.put("totalCourses", totalCourses);
		stats.put("publishedCourses", publishedCourses);
		stats.put("pendingCourses", pendingCourses);

		return stats;
	}
}
