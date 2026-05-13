package com.lms.notification.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * NotificationRequest – DTO for creating a single notification via the REST API.
 *
 * <p>All fields are validated with Bean Validation + Regex before reaching the service layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    /**
     * Target user ID – must be a positive integer.
     */
    @Positive(message = "userId must be a positive integer")
    private int userId;

    /**
     * Notification type constant.
     * Allowed values: ENROLLMENT, PAYMENT, QUIZ_RESULT, CERTIFICATE, COURSE_PUBLISHED.
     * Validated via Regex to enforce the enum-like naming convention.
     */
    @NotBlank(message = "type must not be blank")
    @Pattern(
        regexp = "^[A-Z_]{2,50}$",
        message = "type must use uppercase letters and underscores only"
    )
    private String type;

    /**
     * Short notification title – max 150 characters.
     */
    @NotBlank(message = "title must not be blank")
    @Size(min = 3, max = 150, message = "title must be between 3 and 150 characters")
    private String title;

    /**
     * Full notification message body.
     */
    @NotBlank(message = "message must not be blank")
    @Size(min = 5, max = 1000, message = "message must be between 5 and 1000 characters")
    private String message;

    /**
     * ID of the related domain entity for deep-linking (e.g., courseId, quizId).
     */
    @PositiveOrZero(message = "relatedEntityId must be >= 0")
    private int relatedEntityId;

    /**
     * Type of the related domain entity (e.g., "COURSE", "QUIZ", "PAYMENT").
     * Validated via Regex: uppercase letters only, max 50 chars.
     */
    @NotBlank(message = "relatedEntityType must not be blank")
    @Pattern(
        regexp = "^[A-Z_]{2,50}$",
        message = "relatedEntityType must be uppercase letters/underscores, 2–50 chars"
    )
    private String relatedEntityType;

    /**
     * Optional target email for alerting.
     */
    private String targetEmail;
}
