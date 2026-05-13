package com.lms.lesson_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponseDto {

	private Integer lessonId;
	private Integer courseId;
	private String title;
	private String description;
	private String videoUrl;
	private String resourceUrl;
	private Integer orderIndex;
	private Integer durationMinutes;
	private Boolean preview;
	private Boolean published;
	private LocalDateTime createdAt;
}
