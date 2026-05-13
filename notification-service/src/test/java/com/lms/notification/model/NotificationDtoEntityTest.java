package com.lms.notification.model;

import com.lms.notification.dto.ApiResponse;
import com.lms.notification.dto.BulkNotificationRequest;
import com.lms.notification.dto.EmailRequest;
import com.lms.notification.dto.NotificationRealtimePayload;
import com.lms.notification.dto.NotificationRequest;
import com.lms.notification.entity.EmailLog;
import com.lms.notification.entity.Notification;
import com.lms.notification.exception.NotificationNotFoundException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDtoEntityTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void apiResponseFactoriesAndBuilder_work() {
        ApiResponse<String> success = ApiResponse.success("ok", "payload");
        ApiResponse<String> created = ApiResponse.success(201, "created", "payload");
        ApiResponse<Void> error = ApiResponse.error(400, "bad");

        assertTrue(success.isSuccess());
        assertEquals(200, success.getStatusCode());
        assertEquals(201, created.getStatusCode());
        assertFalse(error.isSuccess());
        assertNotNull(success.getTimestamp());
    }

    @Test
    void notificationRequest_validationAndAccessors_work() {
        NotificationRequest request = NotificationRequest.builder()
                .userId(10)
                .type("COURSE_PUBLISHED")
                .title("Course Live")
                .message("Java course is live")
                .relatedEntityId(7)
                .relatedEntityType("COURSE")
                .targetEmail("user@example.com")
                .build();

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("COURSE", request.getRelatedEntityType());

        request.setType("bad-type");
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void bulkNotificationRequest_validationAndAccessors_work() {
        BulkNotificationRequest request = BulkNotificationRequest.builder()
                .userIds(List.of(1, 2))
                .type("PAYMENT")
                .title("Paid")
                .message("Payment received")
                .relatedEntityId(1)
                .relatedEntityType("PAYMENT")
                .build();

        assertTrue(validator.validate(request).isEmpty());

        request.setUserIds(List.of(-1));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void emailRequestRecordAndEntities_work() {
        EmailRequest emailRequest = EmailRequest.builder()
                .to("user@example.com")
                .subject("Subject")
                .templateName("welcome")
                .templateModel(Map.of("name", "Ravi"))
                .attachmentBase64("abc")
                .attachmentName("file.pdf")
                .build();
        assertEquals("user@example.com", emailRequest.getTo());

        Notification notification = Notification.builder()
                .notificationId(1)
                .userId(10)
                .type("INFO")
                .title("Title")
                .message("Body")
                .relatedEntityId(5)
                .relatedEntityType("COURSE")
                .targetEmail("user@example.com")
                .createdAt(java.time.Instant.now())
                .build();
        notification.setRead(true);
        assertTrue(notification.isRead());
        assertTrue(notification.toString().contains("notificationId=1"));

        EmailLog emailLog = EmailLog.builder()
                .id(1L)
                .recipient("user@example.com")
                .subject("Subject")
                .template("welcome")
                .status(EmailLog.EmailStatus.PENDING)
                .retryCount(1)
                .sentAt(LocalDateTime.now())
                .build();
        emailLog.setErrorMessage("none");
        assertEquals("user@example.com", emailLog.getRecipient());
        assertEquals(EmailLog.EmailStatus.PENDING, emailLog.getStatus());

        NotificationRealtimePayload payload = new NotificationRealtimePayload("CREATED", notification, 4);
        assertEquals("CREATED", payload.event());
        assertEquals(4, payload.unreadCount());

        NotificationNotFoundException ex = new NotificationNotFoundException(99);
        assertEquals(99, ex.getNotificationId());
        assertTrue(ex.getMessage().contains("99"));
    }
}
