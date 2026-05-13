package com.lms.notification.resource;

import com.lms.notification.dto.BulkNotificationRequest;
import com.lms.notification.dto.NotificationRequest;
import com.lms.notification.entity.Notification;
import com.lms.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationResourceTest {

    @Mock
    private NotificationService notifService;

    private NotificationResource resource;
    private Notification notification;
    private UsernamePasswordAuthenticationToken studentAuth;
    private UsernamePasswordAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        resource = new NotificationResource(notifService);
        notification = Notification.builder()
                .notificationId(1)
                .userId(10)
                .type("COURSE_PUBLISHED")
                .title("Course Live")
                .message("Java course is live")
                .build();

        studentAuth = new UsernamePasswordAuthenticationToken(
                "student@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        studentAuth.setDetails(Map.of("userId", 10, "role", "STUDENT"));

        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        adminAuth.setDetails(Map.of("userId", 99, "role", "ADMIN"));
    }

    @Test
    void sendNotification_success() {
        NotificationRequest request = NotificationRequest.builder()
                .userId(10)
                .type("COURSE_PUBLISHED")
                .title("Course Live")
                .message("Java course is live")
                .relatedEntityType("COURSE")
                .build();

        var response = resource.sendNotification(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(notifService).sendNotification(any(Notification.class));
    }

    @Test
    void sendBulkNotification_success() {
        BulkNotificationRequest request = BulkNotificationRequest.builder()
                .userIds(List.of(1, 2))
                .type("INFO")
                .title("Bulk")
                .message("Body text")
                .relatedEntityType("COURSE")
                .build();

        var response = resource.sendBulkNotification(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(notifService).sendBulkNotification(List.of(1, 2), "Bulk", "Body text", "INFO", 0, "COURSE");
    }

    @Test
    void getByUser_successForOwner() {
        when(notifService.getByUser(10)).thenReturn(List.of(notification));

        var response = resource.getByUser(10, studentAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void getByUser_allowsAdmin() {
        when(notifService.getByUser(11)).thenReturn(List.of(notification));

        var response = resource.getByUser(11, adminAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByUser_rejectsDifferentStudent() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resource.getByUser(11, studentAuth));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getMine_success() {
        when(notifService.getByUser(10)).thenReturn(List.of(notification));

        var response = resource.getMine(studentAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void markAsRead_success() {
        when(notifService.getNotification(1)).thenReturn(notification);

        var response = resource.markAsRead(1, studentAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notifService).markAsRead(1);
    }

    @Test
    void markAllRead_success() {
        var response = resource.markAllRead(10, studentAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notifService).markAllRead(10);
    }

    @Test
    void markMyNotificationsRead_success() {
        var response = resource.markMyNotificationsRead(studentAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notifService).markAllRead(10);
    }

    @Test
    void getUnreadCountEndpoints_success() {
        when(notifService.getUnreadCount(10)).thenReturn(5);

        var byUser = resource.getUnreadCount(10, studentAuth);
        var mine = resource.getMyUnreadCount(studentAuth);

        assertEquals(5, byUser.getBody().getData());
        assertEquals(5, mine.getBody().getData());
    }

    @Test
    void deleteNotification_success() {
        when(notifService.getNotification(1)).thenReturn(notification);

        var response = resource.deleteNotification(1, studentAuth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notifService).deleteNotification(1);
    }

    @Test
    void getAll_success() {
        when(notifService.getAll()).thenReturn(List.of(notification));

        var response = resource.getAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData().size());
    }
}
