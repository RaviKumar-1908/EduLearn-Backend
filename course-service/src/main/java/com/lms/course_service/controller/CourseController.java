package com.lms.course_service.controller;

import com.lms.course_service.dto.CourseRequestDto;
import com.lms.course_service.dto.CourseResponseDto;
import com.lms.course_service.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/course", "/api/courses", "/courses"})
@RequiredArgsConstructor
@Slf4j
public class CourseController {

	private final CourseService courseService;

	@PostMapping
	public ResponseEntity<CourseResponseDto> createCourse(@Valid @RequestBody CourseRequestDto courseRequestDto) {
		log.info("Received request to create course");
		CourseResponseDto createdCourse = courseService.createCourse(courseRequestDto);
		return new ResponseEntity<>(createdCourse, HttpStatus.CREATED);
	}

	@GetMapping
	public ResponseEntity<List<CourseResponseDto>> getAllCourses() {
		return new ResponseEntity<>(courseService.getAllCourses(), HttpStatus.OK);
	}

	@GetMapping("/{courseId:\\d+}")
	public ResponseEntity<CourseResponseDto> getCourseById(@PathVariable int courseId) {
		return new ResponseEntity<>(courseService.getCourseById(courseId), HttpStatus.OK);
	}

	@GetMapping("/bulk")
	public ResponseEntity<List<CourseResponseDto>> getCoursesByIds(@RequestParam List<Integer> ids) {
		return new ResponseEntity<>(courseService.getCoursesByIds(ids), HttpStatus.OK);
	}

	@GetMapping("/category/{category}")
	public ResponseEntity<List<CourseResponseDto>> getCoursesByCategory(@PathVariable String category) {
		return new ResponseEntity<>(courseService.getCoursesByCategory(category), HttpStatus.OK);
	}

	@GetMapping("/instructor/{instructorId}")
	public ResponseEntity<List<CourseResponseDto>> getCoursesByInstructor(@PathVariable int instructorId) {
		return new ResponseEntity<>(courseService.getCoursesByInstructor(instructorId), HttpStatus.OK);
	}

	@GetMapping("/level/{level}")
	public ResponseEntity<List<CourseResponseDto>> getCoursesByLevel(@PathVariable String level) {
		return new ResponseEntity<>(courseService.getCoursesByLevel(level), HttpStatus.OK);
	}

	@GetMapping("/published")
	public ResponseEntity<List<CourseResponseDto>> getCoursesByPublishedStatus(@RequestParam(defaultValue = "true") boolean status) {
		return new ResponseEntity<>(courseService.getCoursesByPublishedStatus(status), HttpStatus.OK);
	}

	@GetMapping("/price")
	public ResponseEntity<List<CourseResponseDto>> getCoursesByPrice(@RequestParam double maxPrice) {
		return new ResponseEntity<>(courseService.getCoursesByPrice(maxPrice), HttpStatus.OK);
	}

	@GetMapping("/search")
	public ResponseEntity<List<CourseResponseDto>> searchCourses(@RequestParam(required = false) String keyword) {
		return new ResponseEntity<>(courseService.searchCourses(keyword), HttpStatus.OK);
	}

	@PutMapping("/{courseId}")
	public ResponseEntity<CourseResponseDto> updateCourse(@PathVariable int courseId, @Valid @RequestBody CourseRequestDto courseRequestDto) {
		CourseResponseDto updatedCourse = courseService.updateCourse(courseId, courseRequestDto);
		return new ResponseEntity<>(updatedCourse, HttpStatus.OK);
	}

	@PutMapping("/publish/{courseId}")
	public ResponseEntity<CourseResponseDto> publishCourse(@PathVariable int courseId) {
		CourseResponseDto publishedCourse = courseService.publishCourse(courseId);
		return new ResponseEntity<>(publishedCourse, HttpStatus.OK);
	}

	@DeleteMapping("/{courseId}")
	public ResponseEntity<Void> deleteCourse(@PathVariable int courseId) {
		courseService.deleteCourse(courseId);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/featured")
	public ResponseEntity<List<CourseResponseDto>> getFeaturedCourses() {
		return new ResponseEntity<>(courseService.getFeaturedCourses(), HttpStatus.OK);
	}

	@PutMapping("/{courseId}/duration")
	public ResponseEntity<Void> updateCourseDuration(@PathVariable int courseId, @RequestParam int duration) {
		courseService.updateCourseDuration(courseId, duration);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	// --- Admin Endpoints ---
	@PutMapping("/admin/status/{courseId}")
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<CourseResponseDto> updateCourseStatus(@PathVariable int courseId, @RequestParam String status) {
		return ResponseEntity.ok(courseService.updateCourseStatus(courseId, status));
	}

	@GetMapping("/admin/stats")
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<java.util.Map<String, Object>> getAdminStats() {
		return ResponseEntity.ok(courseService.getAdminStats());
	}
}
