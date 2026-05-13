package com.lms.lesson_service.service.impl;

import com.lms.lesson_service.dto.AiGenerationResult;
import com.lms.lesson_service.dto.LessonRequestDto;
import com.lms.lesson_service.dto.LessonResponseDto;
import com.lms.lesson_service.dto.AskAiRequest;
import com.lms.lesson_service.dto.AskAiResponse;
import com.lms.lesson_service.entity.Lesson;
import com.lms.lesson_service.mapper.LessonMapper;
import com.lms.lesson_service.repository.LessonRepository;
import com.lms.lesson_service.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonServiceImpl implements LessonService {

	private final LessonRepository lessonRepository;
	private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;
	private final com.lms.lesson_service.service.GeminiService geminiService;
	private final org.springframework.web.client.RestTemplate restTemplate;

	@Override
	public LessonResponseDto createLesson(LessonRequestDto requestDto) {
		log.info("Creating lesson '{}' for course {}", requestDto.getTitle(), requestDto.getCourseId());
		Lesson lesson = LessonMapper.toEntity(requestDto);
		lesson.setLessonId(null);
		lesson.setPublished(false);
		return LessonMapper.toResponseDto(lessonRepository.save(lesson));
	}

	@Override
	public LessonResponseDto getLessonById(int lessonId) {
		return LessonMapper.toResponseDto(findLessonEntity(lessonId));
	}

	@Override
	public List<LessonResponseDto> getLessonsByCourse(int courseId) {
		return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
				.map(LessonMapper::toResponseDto)
				.toList();
	}

	@Override
	public List<LessonResponseDto> getPublishedLessonsByCourse(int courseId) {
		return lessonRepository.findByCourseIdAndPublishedTrueOrderByOrderIndexAsc(courseId).stream()
				.map(LessonMapper::toResponseDto)
				.toList();
	}

	@Override
	public List<LessonResponseDto> getPreviewLessonsByCourse(int courseId) {
		return lessonRepository.findByCourseIdAndPreviewTrueOrderByOrderIndexAsc(courseId).stream()
				.map(LessonMapper::toResponseDto)
				.toList();
	}

	@Override
	public LessonResponseDto updateLesson(int lessonId, LessonRequestDto requestDto) {
		Lesson existingLesson = findLessonEntity(lessonId);
		existingLesson.setCourseId(requestDto.getCourseId());
		existingLesson.setTitle(requestDto.getTitle());
		existingLesson.setDescription(requestDto.getDescription());
		existingLesson.setVideoUrl(requestDto.getVideoUrl());
		existingLesson.setResourceUrl(requestDto.getResourceUrl());
		existingLesson.setOrderIndex(requestDto.getOrderIndex());
		existingLesson.setDurationMinutes(requestDto.getDurationMinutes());
		if (requestDto.getPreview() != null) {
			existingLesson.setPreview(requestDto.getPreview());
		}
		return LessonMapper.toResponseDto(lessonRepository.save(existingLesson));
	}

	@Override
	public LessonResponseDto publishLesson(int lessonId) {
		Lesson lesson = findLessonEntity(lessonId);
		lesson.setPublished(!lesson.getPublished());
		Lesson saved = lessonRepository.save(lesson);

		if (saved.getPublished()) {
			Map<String, Object> event = new java.util.HashMap<>();
			event.put("type", "LESSON_PUBLISHED");
			event.put("lessonId", saved.getLessonId());
			event.put("courseId", saved.getCourseId());
			event.put("title", "New Lesson Live! 📚");
			event.put("message", "A new lesson '" + saved.getTitle() + "' is now available for you.");
			event.put("userId", 0); // Broadcaster ID

			// Fetch course details to get instructorId
			try {
				String courseServiceUrl = "http://course-service/api/courses/" + saved.getCourseId();
				java.util.Map<String, Object> course = restTemplate.getForObject(courseServiceUrl, java.util.Map.class);
				if (course != null && course.get("instructorId") != null) {
					event.put("instructorId", course.get("instructorId"));
				}
			} catch (Exception e) {
				log.warn("[RabbitMQ] Could not fetch instructorId for lesson publication notification");
			}

			log.info("[RabbitMQ] ▶ Publishing LESSON_PUBLISHED | lessonId={} courseId={}",
					saved.getLessonId(), saved.getCourseId());
			try {
				rabbitTemplate.convertAndSend(
						"lms.events.exchange", "notification.lesson.published", event);
				log.info("[RabbitMQ] ✔ LESSON_PUBLISHED event published | lessonId={}",
						saved.getLessonId());
			} catch (Exception e) {
				log.error("[RabbitMQ] ✘ Failed to publish LESSON_PUBLISHED | lessonId={} | error={}",
						saved.getLessonId(), e.getMessage(), e);
			}
		}

		return LessonMapper.toResponseDto(saved);
	}

	@Override
	public void deleteLesson(int lessonId) {
		lessonRepository.delete(findLessonEntity(lessonId));
	}

	@Override
	public long getTotalLessonsCount() {
		return lessonRepository.count();
	}

	@Override
	public AskAiResponse askAi(int lessonId, AskAiRequest request) {
		Lesson lesson = findLessonEntity(lessonId);
		String question = request.getQuestion().trim();

		// Build contextual prompt
		String prompt = String.format(
				"You are an expert AI Tutor for an online learning platform. " +
						"The student is currently watching a lesson titled: '%s'.\n" +
						"Lesson Description: %s\n\n" +
						"Student Question: %s\n\n" +
						"Please provide a helpful, concise, and educational response. " +
						"If the question is unrelated to the lesson, politely steer the student back to the lesson context. "
						+
						"Use markdown for formatting when it helps readability.",
				lesson.getTitle(),
				lesson.getDescription(),
				question);

		AiGenerationResult aiResponse = geminiService.generateResponse(prompt);

		return AskAiResponse.builder()
				.response(aiResponse.getResponse())
				.success(aiResponse.isSuccess())
				.build();
	}

	@Override
	public AskAiResponse summarizeLesson(int lessonId) {
		Lesson lesson = findLessonEntity(lessonId);
		String prompt = String.format(
				"Please provide a comprehensive summary of the following lesson.\n" +
						"Title: %s\n" +
						"Description: %s\n\n" +
						"Format the summary using clear headings and bullet points.",
				lesson.getTitle(),
				lesson.getDescription());
		AiGenerationResult aiResponse = geminiService.generateResponse(prompt);
		return AskAiResponse.builder()
				.response(aiResponse.getResponse())
				.success(aiResponse.isSuccess())
				.build();
	}

	@Override
	public AskAiResponse generateQuiz(int lessonId) {
		Lesson lesson = findLessonEntity(lessonId);
		String prompt = String.format(
				"Generate a quiz based on the following lesson content.\n" +
						"Title: %s\n" +
						"Description: %s\n\n" +
						"Provide 5 multiple choice questions with answers. Format the response clearly.",
				lesson.getTitle(),
				lesson.getDescription());
		AiGenerationResult aiResponse = geminiService.generateResponse(prompt);
		return AskAiResponse.builder()
				.response(aiResponse.getResponse())
				.success(aiResponse.isSuccess())
				.build();
	}

	private Lesson findLessonEntity(int lessonId) {
		return lessonRepository.findById(lessonId)
				.orElseThrow(() -> new RuntimeException("Lesson not found with ID: " + lessonId));
	}
}
