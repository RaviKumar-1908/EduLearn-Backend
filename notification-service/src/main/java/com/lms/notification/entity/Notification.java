package com.lms.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Notification – JPA entity representing a single notification record.
 *
 * <p>Tracks:
 * <ul>
 *   <li>Which user the notification targets ({@code userId})</li>
 *   <li>What event triggered it ({@code type} – e.g., ENROLLMENT, PAYMENT, QUIZ_RESULT)</li>
 *   <li>Human-readable title and message body</li>
 *   <li>Read / unread state ({@code isRead})</li>
 *   <li>A deep-link reference ({@code relatedEntityId} + {@code relatedEntityType})</li>
 * </ul>
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_user_id",       columnList = "user_id"),
        @Index(name = "idx_user_is_read",  columnList = "user_id, is_read"),
        @Index(name = "idx_related_entity", columnList = "related_entity_id, related_entity_type")
    }
)
@Data                    // Lombok: generates getters, setters, equals, hashCode, toString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /** Primary key – auto-incremented surrogate key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private int notificationId;

    /**
     * Target user for this notification.
     * References the user record held by Auth-Service (cross-service by ID, not FK).
     */
    @Column(name = "user_id", nullable = false)
    private int userId;

    /**
     * Event type constant.
     * Expected values: ENROLLMENT, PAYMENT, QUIZ_RESULT, CERTIFICATE, COURSE_PUBLISHED.
     */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    /** Short, human-readable subject line shown in the notification bell. */
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    /** Full notification body text. */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Read-state flag.
     * {@code false} = unread (new); {@code true} = user has seen it.
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    /** Timestamp set automatically on INSERT – never updated. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    /**
     * ID of the related domain object (e.g., courseId, quizId, paymentId).
     * Used by the frontend for deep-linking.
     */
    @Column(name = "related_entity_id")
    private int relatedEntityId;

    /**
     * Type of the related domain object (e.g., "COURSE", "QUIZ", "PAYMENT").
     * Paired with {@code relatedEntityId} for polymorphic deep-linking.
     */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    /**
     * Optional target email address.
     * If provided, an email alert will be sent automatically.
     */
    @Column(name = "target_email", length = 150)
    private String targetEmail;

    // ── Custom getter to avoid Lombok conflict with boolean field name ─────────
    /**
     * Returns {@code true} when the notification has been read by the user.
     * Explicit method kept alongside Lombok {@code @Data} for class diagram compliance.
     *
     * @return read state
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * Marks the notification as read or unread.
     *
     * @param read {@code true} to mark as read
     */
    public void setRead(boolean read) {
        this.isRead = read;
    }

    @Override
    public String toString() {
        return "Notification{" +
               "notificationId=" + notificationId +
               ", userId=" + userId +
               ", type='" + type + '\'' +
               ", title='" + title + '\'' +
               ", isRead=" + isRead +
               ", createdAt=" + createdAt +
               ", relatedEntityId=" + relatedEntityId +
               ", relatedEntityType='" + relatedEntityType + '\'' +
               '}';
    }
}
