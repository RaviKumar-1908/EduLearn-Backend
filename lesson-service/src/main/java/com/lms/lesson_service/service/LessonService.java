package com.lms.lesson_service.service;

import com.lms.lesson_service.dto.AskAiRequest;
import com.lms.lesson_service.dto.AskAiResponse;
import com.lms.lesson_service.dto.LessonRequestDto;
import com.lms.lesson_service.dto.LessonResponseDto;

import java.util.List;

public interface LessonService {

	LessonResponseDto createLesson(LessonRequestDto requestDto);

	LessonResponseDto getLessonById(int lessonId);

	List<LessonResponseDto> getLessonsByCourse(int courseId);

	List<LessonResponseDto> getPublishedLessonsByCourse(int courseId);

	List<LessonResponseDto> getPreviewLessonsByCourse(int courseId);

	LessonResponseDto updateLesson(int lessonId, LessonRequestDto requestDto);

	LessonResponseDto publishLesson(int lessonId);

	void deleteLesson(int lessonId);
	
	long getTotalLessonsCount();

	AskAiResponse askAi(int lessonId, AskAiRequest request);
	
	AskAiResponse summarizeLesson(int lessonId);

	AskAiResponse generateQuiz(int lessonId);
}
