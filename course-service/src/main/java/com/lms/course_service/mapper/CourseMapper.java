package com.lms.course_service.mapper;

import com.lms.course_service.dto.CourseRequestDto;
import com.lms.course_service.dto.CourseResponseDto;
import com.lms.course_service.entity.Course;

public class CourseMapper {

    public static Course mapToCourse(CourseRequestDto courseRequestDto) {
        if (courseRequestDto == null) {
            return null;
        }

        Course.CourseBuilder course = Course.builder();

        course.title(courseRequestDto.getTitle());
        course.description(courseRequestDto.getDescription());
        course.category(courseRequestDto.getCategory());
        course.level(courseRequestDto.getLevel());
        course.price(courseRequestDto.getPrice());
        course.instructorId(courseRequestDto.getInstructorId());
        course.thumbnailUrl(courseRequestDto.getThumbnailUrl());
        course.totalDuration(courseRequestDto.getTotalDuration());
        course.language(courseRequestDto.getLanguage());
        course.instructorName(courseRequestDto.getInstructorName());
        course.instructorEmail(courseRequestDto.getInstructorEmail());

        return course.build();
    }

    public static CourseResponseDto mapToCourseResponseDto(Course course) {
        if (course == null) {
            return null;
        }

        CourseResponseDto.CourseResponseDtoBuilder courseResponseDto = CourseResponseDto.builder();

        courseResponseDto.courseId(course.getCourseId());
        courseResponseDto.title(course.getTitle());
        courseResponseDto.description(course.getDescription());
        courseResponseDto.category(course.getCategory());
        courseResponseDto.level(course.getLevel());
        courseResponseDto.price(course.getPrice());
        courseResponseDto.instructorId(course.getInstructorId());
        courseResponseDto.thumbnailUrl(course.getThumbnailUrl());
        courseResponseDto.totalDuration(course.getTotalDuration());
        courseResponseDto.isPublished(course.getIsPublished());
        courseResponseDto.language(course.getLanguage());
        courseResponseDto.createdAt(course.getCreatedAt());
        courseResponseDto.status(course.getStatus());
        courseResponseDto.instructorName(course.getInstructorName());
        courseResponseDto.instructorEmail(course.getInstructorEmail());

        return courseResponseDto.build();
    }
}
