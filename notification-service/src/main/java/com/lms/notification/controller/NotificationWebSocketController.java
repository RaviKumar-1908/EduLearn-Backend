package com.lms.notification.controller;

import com.lms.notification.dto.NotificationRealtimePayload;
import com.lms.notification.entity.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public NotificationWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendNotificationToUser(int userId, Notification notification, int unreadCount) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);
        messagingTemplate.convertAndSend("/topic/notifications/" + userId + "/events",
                new NotificationRealtimePayload("CREATED", notification, unreadCount));
        messagingTemplate.convertAndSend("/topic/notifications/" + userId + "/count", unreadCount);
        messagingTemplate.convertAndSend("/queue/notifications/" + userId,
                new NotificationRealtimePayload("CREATED", notification, unreadCount));
    }

    public void sendUnreadCountUpdate(int userId, int unreadCount) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId + "/count", unreadCount);
        messagingTemplate.convertAndSend("/queue/notifications/" + userId + "/count", unreadCount);
    }
}
