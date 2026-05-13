package com.lms.course_service.mapper;

import com.lms.course_service.dto.CourseRequestDto;
import com.lms.course_service.dto.CourseResponseDto;
import com.lms.course_service.entity.Course;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CourseMapperTest {

    @Test
    void mapToCourse_success() {
        CourseRequestDto dto = new CourseRequestDto();
        dto.setTitle("Java Programming");
        dto.setDescription("Master Java");
        dto.setCategory("Programming");
        dto.setLevel("BEGINNER");
        dto.setPrice(99.99);
        dto.setInstructorId(10);
        dto.setThumbnailUrl("http://image.url");
        dto.setTotalDuration(120);
        dto.setLanguage("English");
        dto.setInstructorName("John Doe");
        dto.setInstructorEmail("john@example.com");

        Course course = CourseMapper.mapToCourse(dto);

        assertNotNull(course);
        assertEquals("Java Programming", course.getTitle());
        assertEquals(99.99, course.getPrice());
        assertEquals("John Doe", course.getInstructorName());
    }

    @Test
    void mapToCourse_null_returnsNull() {
        assertNull(CourseMapper.mapToCourse(null));
    }

    @Test
    void mapToCourseResponseDto_success() {
        Course course = Course.builder()
                .courseId(101)
                .title("Spring Boot")
                .description("Cloud Native")
                .category("Backend")
                .level("ADVANCED")
                .price(199.0)
                .instructorId(5)
                .thumbnailUrl("http://spring.url")
                .totalDuration(300)
                .isPublished(true)
                .language("Spanish")
                .createdAt(LocalDateTime.now())
                .status("ACTIVE")
                .instructorName("Jane Doe")
                .instructorEmail("jane@example.com")
                .build();

        CourseResponseDto dto = CourseMapper.mapToCourseResponseDto(course);

        assertNotNull(dto);
        assertEquals(101, dto.getCourseId());
        assertEquals("Spring Boot", dto.getTitle());
        assertEquals("Spanish", dto.getLanguage());
        assertTrue(dto.getIsPublished());
    }

    @Test
    void mapToCourseResponseDto_null_returnsNull() {
        assertNull(CourseMapper.mapToCourseResponseDto(null));
    }
}
