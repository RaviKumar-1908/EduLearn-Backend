package com.lms.notification.controller;

import com.lms.notification.dto.NotificationRealtimePayload;
import com.lms.notification.entity.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationWebSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendNotificationToUser_pushesAllRealtimeDestinations() {
        NotificationWebSocketController controller = new NotificationWebSocketController(messagingTemplate);
        Notification notification = Notification.builder().notificationId(1).userId(10).title("Title").build();

        controller.sendNotificationToUser(10, notification, 4);

        verify(messagingTemplate).convertAndSend("/topic/notifications/10", notification);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/notifications/10/events"),
                org.mockito.ArgumentMatchers.argThat((NotificationRealtimePayload payload) ->
                        "CREATED".equals(payload.event()) && payload.unreadCount() == 4));
        verify(messagingTemplate).convertAndSend("/topic/notifications/10/count", 4);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/queue/notifications/10"),
                org.mockito.ArgumentMatchers.argThat((NotificationRealtimePayload payload) ->
                        payload.notification().getNotificationId() == 1));
    }

    @Test
    void sendUnreadCountUpdate_pushesTopicAndQueueCounts() {
        NotificationWebSocketController controller = new NotificationWebSocketController(messagingTemplate);

        controller.sendUnreadCountUpdate(10, 2);

        verify(messagingTemplate).convertAndSend("/topic/notifications/10/count", 2);
        verify(messagingTemplate).convertAndSend("/queue/notifications/10/count", 2);
    }
}
