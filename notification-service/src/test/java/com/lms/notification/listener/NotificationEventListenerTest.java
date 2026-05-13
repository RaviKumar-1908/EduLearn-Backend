package com.lms.notification.listener;

import com.lms.notification.entity.Notification;
import com.lms.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RestTemplate restTemplate;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationService, restTemplate);
    }

    @Test
    void onEnrollmentEvent_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("title", "Title");
        payload.put("message", "Message");

        listener.onEnrollmentEvent(payload);

        verify(notificationService).sendNotification(any(Notification.class));
    }

    @Test
    void onPaymentEvent_withEmail_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("email", "test@test.com");
        payload.put("fullName", "John");
        payload.put("courseName", "Java");
        payload.put("amount", "99.99");
        payload.put("transactionId", "TXN123");

        listener.onPaymentEvent(payload);

        verify(notificationService).sendNotification(any(Notification.class));
        verify(notificationService).sendPurchaseConfirmation(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void onAuthEvent_Register_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("type", "AUTH_REGISTER");
        payload.put("email", "test@test.com");
        payload.put("fullName", "John");

        listener.onAuthEvent(payload);

        verify(notificationService).sendWelcomeEmail(eq("test@test.com"), eq("John"));
    }

    @Test
    void onAuthEvent_ForgotPassword_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("type", "AUTH_FORGOT_PASSWORD");
        payload.put("email", "test@test.com");
        payload.put("otp", "123456");

        listener.onAuthEvent(payload);

        verify(notificationService).sendOtpEmail(eq("test@test.com"), eq("123456"));
    }

    @Test
    void onAuthEvent_Login_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("type", "AUTH_LOGIN");
        payload.put("email", "test@test.com");
        payload.put("title", "Login");
        payload.put("message", "Logged in");

        listener.onAuthEvent(payload);

        verify(notificationService).sendEmailAlert(eq("test@test.com"), anyString(), anyString());
    }

    @Test
    void onCourseEvent_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("type", "COURSE_PUBLISHED");

        listener.onCourseEvent(payload);

        verify(notificationService).sendNotification(any(Notification.class));
    }

    @Test
    void onDiscussionEvent_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("type", "DISCUSSION_REPLY");

        listener.onDiscussionEvent(payload);

        verify(notificationService).sendNotification(any(Notification.class));
    }

    @Test
    void sendEmailIfPresent_withRestTemplate_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        // No email in payload

        Map<String, String> userProfile = new HashMap<>();
        userProfile.put("email", "fetched@test.com");

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(userProfile);

        listener.onEnrollmentEvent(payload);

        verify(notificationService).sendEmailAlert(eq("fetched@test.com"), anyString(), anyString());
    }

    @Test
    void hasRecipient_failure_doesNotSend() {
        Map<String, Object> payload = new HashMap<>();
        // No userId

        listener.onEnrollmentEvent(payload);

        verify(notificationService, never()).sendNotification(any());
    }
}
