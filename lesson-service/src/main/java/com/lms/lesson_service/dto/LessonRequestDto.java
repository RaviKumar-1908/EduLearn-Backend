package com.lms.lesson_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequestDto {

	@NotNull(message = "Course ID is required")
	private Integer courseId;

	@NotBlank(message = "Title is required")
	private String title;

	private String description;

	private String videoUrl;

	private String resourceUrl;

	@NotNull(message = "Order index is required")
	@Min(value = 1, message = "Order index must be at least 1")
	private Integer orderIndex;

	@Min(value = 1, message = "Duration must be positive")
	private Integer durationMinutes;

	private Boolean preview;
}
