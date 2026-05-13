package com.lms.notification.exception;

/**
 * NotificationNotFoundException – thrown when a notification lookup by ID yields no result.
 *
 * <p>Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class NotificationNotFoundException extends RuntimeException {

    private final int notificationId;

    /**
     * @param notificationId the ID that was not found
     */
    public NotificationNotFoundException(int notificationId) {
        super("Notification not found with ID: " + notificationId);
        this.notificationId = notificationId;
    }

    public int getNotificationId() {
        return notificationId;
    }
}
