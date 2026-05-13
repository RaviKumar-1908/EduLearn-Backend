package com.lms.lesson_service.controller;

import com.lms.lesson_service.dto.LessonRequestDto;
import com.lms.lesson_service.dto.LessonResponseDto;
import com.lms.lesson_service.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({ "/api/lesson", "/lesson" })
@RequiredArgsConstructor
public class LessonController {

	private final LessonService lessonService;

	@PostMapping
	public ResponseEntity<LessonResponseDto> createLesson(@Valid @RequestBody LessonRequestDto requestDto) {
		return new ResponseEntity<>(lessonService.createLesson(requestDto), HttpStatus.CREATED);
	}

	@GetMapping("/{lessonId}")
	public ResponseEntity<LessonResponseDto> getLessonById(@PathVariable int lessonId) {
		return ResponseEntity.ok(lessonService.getLessonById(lessonId));
	}

	@GetMapping("/course/{courseId}")
	public ResponseEntity<List<LessonResponseDto>> getLessonsByCourse(@PathVariable int courseId) {
		return ResponseEntity.ok(lessonService.getLessonsByCourse(courseId));
	}

	@GetMapping("/course/{courseId}/published")
	public ResponseEntity<List<LessonResponseDto>> getPublishedLessonsByCourse(@PathVariable int courseId) {
		return ResponseEntity.ok(lessonService.getPublishedLessonsByCourse(courseId));
	}

	@GetMapping("/course/{courseId}/preview")
	public ResponseEntity<List<LessonResponseDto>> getPreviewLessonsByCourse(@PathVariable int courseId) {
		return ResponseEntity.ok(lessonService.getPreviewLessonsByCourse(courseId));
	}

	@PutMapping("/{lessonId}")
	public ResponseEntity<LessonResponseDto> updateLesson(@PathVariable int lessonId,
			@Valid @RequestBody LessonRequestDto requestDto) {
		return ResponseEntity.ok(lessonService.updateLesson(lessonId, requestDto));
	}

	@PutMapping("/publish/{lessonId}")
	public ResponseEntity<LessonResponseDto> publishLesson(@PathVariable int lessonId) {
		return ResponseEntity.ok(lessonService.publishLesson(lessonId));
	}

	@DeleteMapping("/{lessonId}")
	public ResponseEntity<Void> deleteLesson(@PathVariable int lessonId) {
		lessonService.deleteLesson(lessonId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/count")
	public ResponseEntity<Long> getTotalLessonsCount() {
		return ResponseEntity.ok(lessonService.getTotalLessonsCount());
	}

	@PostMapping("/{lessonId}/ask-ai")
	public ResponseEntity<com.lms.lesson_service.dto.AskAiResponse> askAi(
			@PathVariable int lessonId,
			@Valid @RequestBody com.lms.lesson_service.dto.AskAiRequest request) {
		return ResponseEntity.ok(lessonService.askAi(lessonId, request));
	}

	@GetMapping("/{lessonId}/summarize")
	public ResponseEntity<com.lms.lesson_service.dto.AskAiResponse> summarizeLesson(@PathVariable int lessonId) {
		return ResponseEntity.ok(lessonService.summarizeLesson(lessonId));
	}

	@GetMapping("/{lessonId}/generate-quiz")
	public ResponseEntity<com.lms.lesson_service.dto.AskAiResponse> generateQuiz(@PathVariable int lessonId) {
		return ResponseEntity.ok(lessonService.generateQuiz(lessonId));
	}
}
