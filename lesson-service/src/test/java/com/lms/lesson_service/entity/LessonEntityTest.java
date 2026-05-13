package com.lms.lesson_service.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LessonEntityTest {

    @Test
    void testLessonBuilderAndGetters() {
        Lesson lesson = Lesson.builder()
                .lessonId(1)
                .courseId(10)
                .title("Unit Test")
                .description("Desc")
                .published(true)
                .preview(false)
                .orderIndex(5)
                .durationMinutes(45)
                .build();

        assertEquals(1, lesson.getLessonId());
        assertEquals(10, lesson.getCourseId());
        assertEquals("Unit Test", lesson.getTitle());
        assertEquals("Desc", lesson.getDescription());
        assertTrue(lesson.getPublished());
        assertFalse(lesson.getPreview());
        assertEquals(5, lesson.getOrderIndex());
        assertEquals(45, lesson.getDurationMinutes());
    }

    @Test
    void testNoArgsConstructor() {
        Lesson lesson = new Lesson();
        assertNull(lesson.getLessonId());
        assertNull(lesson.getTitle());
    }
}
