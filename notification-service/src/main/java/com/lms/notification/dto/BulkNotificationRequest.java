package com.lms.notification.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * BulkNotificationRequest – DTO for dispatching the same notification to multiple users.
 *
 * <p>Used by the {@code /notifications/bulk} endpoint and internally by
 * {@code NotificationService#sendBulkNotification}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkNotificationRequest {

    /**
     * List of target user IDs.
     * Must contain at least one ID and no null elements.
     */
    @NotEmpty(message = "userIds list must not be empty")
    private List<@Positive(message = "Each userId must be a positive integer") Integer> userIds;

    /**
     * Notification type – same regex constraint as {@link NotificationRequest}.
     */
    @NotBlank(message = "type must not be blank")
    @Pattern(
        regexp = "^[A-Z_]{2,50}$",
        message = "type must use uppercase letters and underscores only"
    )
    private String type;

    /** Short title shown in the notification bell. */
    @NotBlank(message = "title must not be blank")
    @Size(min = 3, max = 150, message = "title must be between 3 and 150 characters")
    private String title;

    /** Full notification body. */
    @NotBlank(message = "message must not be blank")
    @Size(min = 5, max = 1000, message = "message must be between 5 and 1000 characters")
    private String message;

    /** Related entity ID for deep-linking (0 if not applicable). */
    @PositiveOrZero(message = "relatedEntityId must be >= 0")
    private int relatedEntityId;

    /** Related entity type for deep-linking. */
    @NotBlank(message = "relatedEntityType must not be blank")
    @Pattern(
        regexp = "^[A-Z_]{2,50}$",
        message = "relatedEntityType must be uppercase letters/underscores, 2–50 chars"
    )
    private String relatedEntityType;
}
