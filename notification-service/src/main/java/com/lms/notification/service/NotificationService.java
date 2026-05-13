package com.lms.notification.service;

import com.lms.notification.entity.Notification;

import java.util.List;

/**
 * NotificationService – service-layer contract for all notification operations.
 *
 * <p>Declares:
 * <ul>
 *   <li>Single and bulk notification dispatch</li>
 *   <li>Read-state management (mark one / mark all)</li>
 *   <li>User-scoped and global retrieval</li>
 *   <li>Unread badge count</li>
 *   <li>Deletion</li>
 *   <li>Email alert dispatch</li>
 * </ul>
 *
 * <p>Implemented by {@link com.lms.notification.service.impl.NotificationServiceImpl}.
 *
 * @author LMS Team
 */
public interface NotificationService {

    /**
     * Persists a single notification and triggers an email alert to the target user
     * if the notification type warrants one.
     *
     * @param notification fully-populated {@link Notification} object (not yet persisted)
     */
    void sendNotification(Notification notification);

    /**
     * Dispatches identical notifications to multiple users in a single call.
     * Typically triggered by course-publication events where all enrolled users
     * need to be notified at once.
     *
     * @param userIds          list of target user IDs
     * @param title            notification title
     * @param message          notification body
     * @param type             event type constant
     * @param relatedEntityId  domain entity ID for deep-linking
     * @param relatedEntityType domain entity type label
     */
    void sendBulkNotification(List<Integer> userIds,
                              String title,
                              String message,
                              String type,
                              int relatedEntityId,
                              String relatedEntityType);

    /**
     * Marks a single notification as read.
     *
     * @param notificationId ID of the notification to mark
     * @throws com.lms.notification.exception.NotificationNotFoundException if ID does not exist
     */
    void markAsRead(int notificationId);

    /**
     * Marks all unread notifications for a given user as read in one batch update.
     *
     * @param userId target user ID
     */
    void markAllRead(int userId);

    /**
     * Retrieves all notifications for a user, ordered newest-first.
     *
     * @param userId target user ID
     * @return list of notifications (may be empty)
     */
    List<Notification> getByUser(int userId);

    /**
     * Retrieves a single notification by its ID.
     *
     * @param notificationId target notification ID
     * @return notification entity
     * @throws com.lms.notification.exception.NotificationNotFoundException if ID does not exist
     */
    Notification getNotification(int notificationId);

    /**
     * Returns the count of unread notifications for a user.
     * Used to populate the notification bell badge in the UI.
     *
     * @param userId target user ID
     * @return unread count (>= 0)
     */
    int getUnreadCount(int userId);

    /**
     * Hard-deletes a single notification by its ID.
     *
     * @param notificationId ID of the notification to delete
     * @throws com.lms.notification.exception.NotificationNotFoundException if ID does not exist
     */
    void deleteNotification(int notificationId);

    /**
     * Hard-deletes all notifications for a given user.
     *
     * @param userId target user ID
     */
    void deleteAllForUser(int userId);

    /**
     * Sends an email alert for a notification event.
     * Called internally after persisting a notification when email alerting applies.
     *
     * @param toEmail  recipient email address (validated with Regex in impl)
     * @param subject  email subject line
     * @param body     email body content
     */
    void sendEmailAlert(String toEmail, String subject, String body);

    /**
     * Retrieves every notification in the system (admin-only).
     *
     * @return full list of all notifications
     */
    List<Notification> getAll();

    // --- Specialized Email Methods ---
    void sendWelcomeEmail(String toEmail, String name);
    void sendOtpEmail(String toEmail, String otp);
    void sendPurchaseConfirmation(String toEmail, String name, String courseName, String instructorName, String amount, String orderId, String thumbnailUrl);
    void sendPasswordResetEmail(String toEmail, String resetUrl);
    void sendCertificateEmail(com.lms.notification.dto.EmailRequest request);

    /**
     * Dispatches a notification to all users with the ADMIN role.
     * Fetches admin IDs from auth-service and sends bulk notification.
     */
    void notifyAdmins(String title, String message, String type, int relatedEntityId, String relatedEntityType);
}
