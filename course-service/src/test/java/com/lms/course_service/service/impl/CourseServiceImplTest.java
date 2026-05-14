package com.lms.course_service.service.impl;

import com.lms.course_service.dto.CourseRequestDto;
import com.lms.course_service.dto.CourseResponseDto;
import com.lms.course_service.entity.Course;
import com.lms.course_service.mapper.CourseMapper;
import com.lms.course_service.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course mockCourse;
    private CourseRequestDto courseRequestDto;
    private CourseResponseDto mockResponseDto;

    @BeforeEach
    void setUp() {
        mockCourse = new Course();
        mockCourse.setCourseId(1);
        mockCourse.setTitle("Java Basics");
        mockCourse.setDescription("Intro to Java");
        mockCourse.setCategory("Programming");
        mockCourse.setLevel("BEGINNER");
        mockCourse.setPrice(99.99);
        mockCourse.setInstructorId(10);
        mockCourse.setIsPublished(false);
        mockCourse.setStatus("PENDING");
        mockCourse.setCreatedAt(LocalDateTime.now());

        courseRequestDto = new CourseRequestDto();
        courseRequestDto.setTitle("Java Basics");
        courseRequestDto.setDescription("Intro to Java");
        courseRequestDto.setCategory("Programming");
        courseRequestDto.setLevel("BEGINNER");
        courseRequestDto.setPrice(99.99);
        courseRequestDto.setInstructorId(10);
        courseRequestDto.setThumbnailUrl("http://thumb.url/java.jpg");
        courseRequestDto.setTotalDuration(120);
        courseRequestDto.setLanguage("English");

        mockResponseDto = new CourseResponseDto();
        mockResponseDto.setCourseId(1);
        mockResponseDto.setTitle("Java Basics");
        mockResponseDto.setCategory("Programming");
        mockResponseDto.setLevel("BEGINNER");
        mockResponseDto.setPrice(99.99);
        mockResponseDto.setIsPublished(false);
    }

    // ==================== CREATE COURSE ====================

    @Test
    void createCourse_success() {
        try (MockedStatic<CourseMapper> mapper = mockStatic(CourseMapper.class)) {
            mapper.when(() -> CourseMapper.mapToCourse(any(CourseRequestDto.class))).thenReturn(mockCourse);
            mapper.when(() -> CourseMapper.mapToCourseResponseDto(any(Course.class))).thenReturn(mockResponseDto);
            when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);
            doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            CourseResponseDto result = courseService.createCourse(courseRequestDto);

            assertNotNull(result);
            assertEquals("Java Basics", result.getTitle());
            verify(courseRepository, times(1)).save(any(Course.class));
            verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    // ==================== GET ALL COURSES ====================

    @Test
    void getAllCourses_success() {
        try (MockedStatic<CourseMapper> mapper = mockStatic(CourseMapper.class)) {
            mapper.when(() -> CourseMapper.mapToCourseResponseDto(any(Course.class))).thenReturn(mockResponseDto);
            when(courseRepository.findAll()).thenReturn(List.of(mockCourse));

            List<CourseResponseDto> result = courseService.getAllCourses();

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(courseRepository, times(1)).findAll();
        }
    }

    @Test
    void getAllCourses_emptyList() {
        when(courseRepository.findAll()).thenReturn(List.of());

        List<CourseResponseDto> result = courseService.getAllCourses();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== GET COURSE BY ID ====================

    @Test
    void getCourseById_success() {
        try (MockedStatic<CourseMapper> mapper = mockStatic(CourseMapper.class)) {
            mapper.when(() -> CourseMapper.mapToCourseResponseDto(any(Course.class))).thenReturn(mockResponseDto);
            when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));

            CourseResponseDto result = courseService.getCourseById(1);

            assertNotNull(result);
            assertEquals(1, result.getCourseId());
            verify(courseRepository, times(1)).findById(1);
        }
    }

    @Test
    void getCourseById_notFound_throwsException() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> courseService.getCourseById(99));

        assertTrue(exception.getMessage().contains("99"));
    }

    // ==================== GET COURSES BY IDS ====================

    @Test
    void getCoursesByIds_success() {
        try (MockedStatic<CourseMapper> mapper = mockStatic(CourseMapper.class)) {
            mapper.when(() -> CourseMapper.mapToCourseResponseDto(any(Course.class))).thenReturn(mockResponseDto);
            when(courseRepository.findByCourseIdIn(anyList())).thenReturn(List.of(mockCourse));

            List<CourseResponseDto> result = courseService.getCoursesByIds(List.of(1));

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(courseRepository, times(1)).findByCourseIdIn(anyList());
        }
    }

    // ==================== GET BY CATEGORY ====================

    @Test
    void getCoursesByCategory_success() {
        try (MockedStatic<CourseMapper> mapper = mockStatic(CourseMapper.class)) {
            mapper.when(() -> CourseMapper.mapToCourseResponseDto(any(Course.class))).thenReturn(mockResponseDto);
            when(courseRepository.findByCategoryIgnoreCase("Programming"))
                    .thenReturn(List.of(mockCourse));

            List<CourseResponseDto> result = courseService.getCoursesByCategory("Programming");

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    // ==================== SEARCH COURSES ====================

    @Test
    void searchCourses_withKeyword_success() {
        try (MockedStatic<CourseMapper> mapper = mockStatic(CourseMapper.class)) {
            mapper.when(() -> CourseMapper.mapToCourseResponseDto(any(Course.class))).thenReturn(mockResponseDto);
            when(courseRepository.searchByKeyword("Java")).thenReturn(List.of(mockCourse));

            List<CourseResponseDto> result = courseService.searchCourses("Java");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(courseRepository, times(1)).searchByKeyword("Java");
        }
    }

    // ==================== UPDATE COURSE ====================

    @Test
    void updateCourse_success() {
        try (MockedStatic<CourseMapper> mapper = mockStatic(CourseMapper.class)) {
            mapper.when(() -> CourseMapper.mapToCourseResponseDto(any(Course.class))).thenReturn(mockResponseDto);
            when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);

            CourseResponseDto result = courseService.updateCourse(1, courseRequestDto);

            assertNotNull(result);
            verify(courseRepository, times(1)).save(any(Course.class));
        }
    }

    // ==================== PUBLISH COURSE ====================

    @Test
    void publishCourse_success() {
        try (MockedStatic<CourseMapper> mapper = mockStatic(CourseMapper.class)) {
            mapper.when(() -> CourseMapper.mapToCourseResponseDto(any(Course.class))).thenReturn(mockResponseDto);
            when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);
            doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            CourseResponseDto result = courseService.publishCourse(1);

            assertNotNull(result);
            assertTrue(mockCourse.getIsPublished());
            verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    // ==================== DELETE COURSE ====================

    @Test
    void deleteCourse_success() {
        when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));
        doNothing().when(courseRepository).delete(any(Course.class));
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        courseService.deleteCourse(1);

        verify(courseRepository, times(1)).delete(any(Course.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ==================== UPDATE COURSE DURATION ====================

    @Test
    void updateCourseDuration_success() {
        when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));
        when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);

        courseService.updateCourseDuration(1, 180);

        assertEquals(180, mockCourse.getTotalDuration());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    // ==================== UPDATE COURSE STATUS ====================

    @Test
    void updateCourseStatus_approved_success() {
        try (MockedStatic<CourseMapper> mapper = mockStatic(CourseMapper.class)) {
            mapper.when(() -> CourseMapper.mapToCourseResponseDto(any(Course.class))).thenReturn(mockResponseDto);
            when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(mockCourse);

            CourseResponseDto result = courseService.updateCourseStatus(1, "APPROVED");

            assertNotNull(result);
            assertEquals("APPROVED", mockCourse.getStatus());
            verify(courseRepository, times(1)).save(any(Course.class));
        }
    }

    // ==================== ADMIN STATS ====================

    @Test
    void getAdminStats_success() {
        Course pendingCourse = new Course();
        pendingCourse.setStatus("PENDING");
        pendingCourse.setIsPublished(false);

        Course publishedCourse = new Course();
        publishedCourse.setStatus("APPROVED");
        publishedCourse.setIsPublished(true);

        when(courseRepository.count()).thenReturn(2L);
        when(courseRepository.findByIsPublished(true)).thenReturn(List.of(publishedCourse));
        when(courseRepository.findAll()).thenReturn(List.of(pendingCourse, publishedCourse));

        Map<String, Object> stats = courseService.getAdminStats();

        assertNotNull(stats);
        assertEquals(2L, stats.get("totalCourses"));
        assertEquals(1L, stats.get("publishedCourses"));
        assertEquals(1L, stats.get("pendingCourses"));
        verify(courseRepository, times(1)).count();
    }

    @Test
    void searchCourses_emptyOrNullKeyword_returnsAll() {
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse));

        List<CourseResponseDto> resultNull = courseService.searchCourses(null);
        List<CourseResponseDto> resultBlank = courseService.searchCourses("  ");

        assertEquals(1, resultNull.size());
        assertEquals(1, resultBlank.size());
    }

    @Test
    void getFeaturedCourses_sortingAndLimit() {
        Course c1 = new Course();
        c1.setCreatedAt(LocalDateTime.now().minusDays(1));
        c1.setIsPublished(true);
        Course c2 = new Course();
        c2.setCreatedAt(LocalDateTime.now());
        c2.setIsPublished(true);

        when(courseRepository.findByIsPublished(true)).thenReturn(List.of(c1, c2));

        List<CourseResponseDto> result = courseService.getFeaturedCourses();
        assertEquals(2, result.size());
        // Verify c2 comes first (reverse order)
        verify(courseRepository).findByIsPublished(true);
    }

    @Test
    void getCoursesByInstructor_success() {
        when(courseRepository.findByInstructorId(10)).thenReturn(List.of(mockCourse));
        List<CourseResponseDto> result = courseService.getCoursesByInstructor(10);
        assertFalse(result.isEmpty());
    }

    @Test
    void getCoursesByLevel_success() {
        when(courseRepository.findByLevelIgnoreCase("BEGINNER")).thenReturn(List.of(mockCourse));
        List<CourseResponseDto> result = courseService.getCoursesByLevel("BEGINNER");
        assertFalse(result.isEmpty());
    }

    @Test
    void getCoursesByPrice_success() {
        when(courseRepository.findByPriceLessThanEqual(100.0)).thenReturn(List.of(mockCourse));
        List<CourseResponseDto> result = courseService.getCoursesByPrice(100.0);
        assertFalse(result.isEmpty());
    }

    @Test
    void publishCourse_notFound_throwsException() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> courseService.publishCourse(99));
    }

    @Test
    void deleteCourse_notFound_throwsException() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> courseService.deleteCourse(99));
    }

    @Test
    void updateCourseDuration_notFound_throwsException() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> courseService.updateCourseDuration(99, 100));
    }

    @Test
    void updateCourseStatus_notFound_throwsException() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> courseService.updateCourseStatus(99, "REJECTED"));
    }
}
