package com.lms.course_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDto {
    private Integer courseId;
    private String title;
    private String description;
    private String category;
    private String level;
    private Double price;
    private Integer instructorId;
    private String thumbnailUrl;
    private Integer totalDuration;
    private Boolean isPublished;
    private String language;
    private LocalDateTime createdAt;
    private String status;
    private String instructorName;
    private String instructorEmail;
}
