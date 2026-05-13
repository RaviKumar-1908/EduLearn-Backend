package com.lms.notification.repository;

import com.lms.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NotificationRepository – Spring Data JPA repository for {@link Notification} persistence.
 *
 * <p>Extends {@link JpaRepository} to inherit standard CRUD operations.
 * Custom finder and mutation methods are declared below using JPQL / derived queries.
 *
 * @author LMS Team
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    /**
     * Retrieves all notifications for a given user, ordered newest-first.
     *
     * @param userId target user ID
     * @return list of notifications (may be empty)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(int userId);

    /**
     * Retrieves notifications for a user filtered by read/unread state.
     *
     * @param userId target user ID
     * @param isRead {@code false} for unread, {@code true} for read
     * @return filtered notification list
     */
    List<Notification> findByUserIdAndIsRead(int userId, boolean isRead);

    /**
     * Counts notifications for a user with a specific read state.
     * Used to power the unread badge count in the UI notification bell.
     *
     * @param userId target user ID
     * @param isRead read state to count
     * @return count of matching records
     */
    int countByUserIdAndIsRead(int userId, boolean isRead);

    /**
     * Retrieves all notifications of a specific event type across all users.
     * Useful for admin-level auditing.
     *
     * @param type event type constant (e.g., "ENROLLMENT", "PAYMENT")
     * @return list of matching notifications
     */
    List<Notification> findByType(String type);

    /**
     * Retrieves all notifications linked to a specific domain entity.
     * Enables deep-link resolution (e.g., all notifications about courseId=5).
     *
     * @param relatedEntityId ID of the domain entity
     * @return list of notifications referencing this entity
     */
    List<Notification> findByRelatedEntityId(int relatedEntityId);

    /**
     * Hard-deletes a notification by its primary key.
     *
     * @param notificationId notification to delete
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.notificationId = :notificationId")
    void deleteByNotificationId(@Param("notificationId") int notificationId);

    /**
     * Bulk-marks all unread notifications for a user as read in a single DB round-trip.
     *
     * @param userId target user ID
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") int userId);

    /**
     * Hard-deletes all notifications for a given user.
     *
     * @param userId target user ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    void deleteByUserId(@Param("userId") int userId);
}
