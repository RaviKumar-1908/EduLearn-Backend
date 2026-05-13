package com.lms.course_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer courseId;

	@NotBlank(message = "Title is required")
	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@NotBlank(message = "Category is required")
	@Column(nullable = false)
	private String category;

	@NotBlank(message = "Level is required")
	@Column(nullable = false)
	private String level;

	@NotNull(message = "Price is required")
	@DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
	@Column(nullable = false)
	private Double price;

	@NotNull(message = "Instructor ID is required")
	@Column(nullable = false)
	private Integer instructorId;

	@Column(length = 100)
	private String instructorName;

	@Column(length = 100)
	private String instructorEmail;

	private String thumbnailUrl;

	private Integer totalDuration;

	@Builder.Default
	private Boolean isPublished = false;

	private String language;

	@Builder.Default
	private String status = "APPROVED";

	@Builder.Default
	private Double averageRating = 0.0;

	@Builder.Default
	private Integer ratingCount = 0;

	private LocalDateTime createdAt;

	@PrePersist
	public void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (isPublished == null) {
			isPublished = false;
		}
	}
}
