package com.lms.notification.dto;

import com.lms.notification.entity.Notification;

/**
 * Real-time payload pushed over WebSocket so clients can update both the list
 * view and the unread badge from a single event.
 */
public record NotificationRealtimePayload(
        String event,
        Notification notification,
        Integer unreadCount
) {
}
