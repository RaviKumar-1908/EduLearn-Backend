package com.lms.lesson_service.mapper;

import com.lms.lesson_service.dto.LessonRequestDto;
import com.lms.lesson_service.dto.LessonResponseDto;
import com.lms.lesson_service.entity.Lesson;

public final class LessonMapper {

	private LessonMapper() {
	}

	public static Lesson toEntity(LessonRequestDto requestDto) {
		return Lesson.builder()
				.courseId(requestDto.getCourseId())
				.title(requestDto.getTitle())
				.description(requestDto.getDescription())
				.videoUrl(requestDto.getVideoUrl())
				.resourceUrl(requestDto.getResourceUrl())
				.orderIndex(requestDto.getOrderIndex())
				.durationMinutes(requestDto.getDurationMinutes())
				.preview(requestDto.getPreview())
				.build();
	}

	public static LessonResponseDto toResponseDto(Lesson lesson) {
		return LessonResponseDto.builder()
				.lessonId(lesson.getLessonId())
				.courseId(lesson.getCourseId())
				.title(lesson.getTitle())
				.description(lesson.getDescription())
				.videoUrl(lesson.getVideoUrl())
				.resourceUrl(lesson.getResourceUrl())
				.orderIndex(lesson.getOrderIndex())
				.durationMinutes(lesson.getDurationMinutes())
				.preview(lesson.getPreview())
				.published(lesson.getPublished())
				.createdAt(lesson.getCreatedAt())
				.build();
	}
}
