package com.discussion.messaging;

import com.discussion.config.RabbitMQConfig;
import com.discussion.entity.DiscussionThread;
import com.discussion.entity.Reply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class DiscussionNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(DiscussionNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    public DiscussionNotificationPublisher(RabbitTemplate rabbitTemplate, RestTemplate restTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
    }

    public void publishThreadCreated(DiscussionThread thread) {
        Integer instructorId = fetchInstructorId(thread.getCourseId());
        if (instructorId == null || instructorId <= 0 || instructorId == thread.getAuthorId()) {
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("userId", instructorId);
        event.put("title", "New Course Discussion");
        event.put("message", buildThreadMessage(thread));
        event.put("type", "DISCUSSION_THREAD_CREATED");
        event.put("relatedEntityId", thread.getThreadId());
        event.put("relatedEntityType", "DISCUSSION_THREAD");
        event.put("courseId", thread.getCourseId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.LMS_EVENTS_EXCHANGE, "notification.discussion.thread.created", event);
        log.info("Published discussion thread notification for instructorId={} threadId={}", instructorId, thread.getThreadId());
    }

    public void publishReplyCreated(DiscussionThread thread, Reply reply) {
        if (thread.getAuthorId() <= 0 || thread.getAuthorId() == reply.getAuthorId()) {
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("userId", thread.getAuthorId());
        event.put("title", "New Reply On Your Discussion");
        event.put("message", buildReplyMessage(thread, reply));
        event.put("type", "DISCUSSION_REPLY_CREATED");
        event.put("relatedEntityId", thread.getThreadId());
        event.put("relatedEntityType", "DISCUSSION_THREAD");
        event.put("courseId", thread.getCourseId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.LMS_EVENTS_EXCHANGE, "notification.discussion.reply.created", event);
        log.info("Published discussion reply notification for authorId={} threadId={}", thread.getAuthorId(), thread.getThreadId());
    }

    private Integer fetchInstructorId(int courseId) {
        try {
            Map<?, ?> course = restTemplate.getForObject("http://course-service/api/courses/" + courseId, Map.class);
            Object instructorId = course != null ? course.get("instructorId") : null;
            return instructorId != null ? Integer.parseInt(instructorId.toString()) : null;
        } catch (Exception ex) {
            log.warn("Could not resolve instructor for courseId={}: {}", courseId, ex.getMessage());
            return null;
        }
    }

    private String buildThreadMessage(DiscussionThread thread) {
        String author = thread.getAuthorName() != null && !thread.getAuthorName().isBlank()
                ? thread.getAuthorName()
                : "A student";
        return author + " started a new discussion: " + thread.getTitle();
    }

    private String buildReplyMessage(DiscussionThread thread, Reply reply) {
        String author = reply.getAuthorName() != null && !reply.getAuthorName().isBlank()
                ? reply.getAuthorName()
                : "Someone";
        return author + " replied to your discussion: " + thread.getTitle();
    }
}
