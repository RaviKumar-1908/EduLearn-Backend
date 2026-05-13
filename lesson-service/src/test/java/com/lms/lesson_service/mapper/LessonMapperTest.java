package com.lms.lesson_service.mapper;

import com.lms.lesson_service.dto.LessonRequestDto;
import com.lms.lesson_service.dto.LessonResponseDto;
import com.lms.lesson_service.entity.Lesson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LessonMapperTest {

    @Test
    void toEntity_success() {
        LessonRequestDto dto = new LessonRequestDto();
        dto.setCourseId(10);
        dto.setTitle("Title");
        dto.setDescription("Desc");
        dto.setVideoUrl("http://video");
        dto.setResourceUrl("http://res");
        dto.setOrderIndex(1);
        dto.setDurationMinutes(20);
        dto.setPreview(true);

        Lesson entity = LessonMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(dto.getCourseId(), entity.getCourseId());
        assertEquals(dto.getTitle(), entity.getTitle());
        assertEquals(dto.getDescription(), entity.getDescription());
        assertEquals(dto.getVideoUrl(), entity.getVideoUrl());
        assertEquals(dto.getResourceUrl(), entity.getResourceUrl());
        assertEquals(dto.getOrderIndex(), entity.getOrderIndex());
        assertEquals(dto.getDurationMinutes(), entity.getDurationMinutes());
        assertEquals(dto.getPreview(), entity.getPreview());
    }

    @Test
    void toResponseDto_success() {
        Lesson entity = Lesson.builder()
                .lessonId(1)
                .courseId(10)
                .title("Title")
                .published(true)
                .preview(false)
                .build();

        LessonResponseDto dto = LessonMapper.toResponseDto(entity);

        assertNotNull(dto);
        assertEquals(entity.getLessonId(), dto.getLessonId());
        assertEquals(entity.getTitle(), dto.getTitle());
        assertEquals(entity.getPublished(), dto.getPublished());
        assertEquals(entity.getPreview(), dto.getPreview());
    }
}
