package com.lms.notification.service.impl;

import com.lms.notification.controller.NotificationWebSocketController;
import com.lms.notification.dto.EmailRequest;
import com.lms.notification.entity.Notification;
import com.lms.notification.exception.NotificationNotFoundException;
import com.lms.notification.repository.NotificationRepository;
import com.lms.notification.service.AsyncEmailProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AsyncEmailProducer emailProducer;

    @Mock
    private NotificationWebSocketController notificationWebSocketController;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        mockNotification = Notification.builder()
                .notificationId(1)
                .userId(10)
                .title("Test Notification")
                .message("Message content")
                .type("INFO")
                .isRead(false)
                .targetEmail("test@example.com")
                .build();
    }

    @Test
    void sendNotification_success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);
        when(notificationRepository.countByUserIdAndIsRead(10, false)).thenReturn(1);
        
        notificationService.sendNotification(mockNotification);

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(emailProducer, times(1)).queueEmail(any());
        verify(notificationWebSocketController).sendNotificationToUser(10, mockNotification, 1);
    }

    @Test
    void sendBulkNotification_success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);
        when(notificationRepository.countByUserIdAndIsRead(10, false)).thenReturn(1);

        notificationService.sendBulkNotification(List.of(1, 2), "Bulk", "Body", "PROMO", 0, null);

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void markAsRead_success() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(mockNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);
        when(notificationRepository.countByUserIdAndIsRead(10, false)).thenReturn(0);

        notificationService.markAsRead(1);

        assertTrue(mockNotification.isRead());
        verify(notificationRepository, times(1)).save(mockNotification);
        verify(notificationWebSocketController).sendUnreadCountUpdate(10, 0);
    }

    @Test
    void getUnreadCount_success() {
        when(notificationRepository.countByUserIdAndIsRead(10, false)).thenReturn(5);

        int count = notificationService.getUnreadCount(10);

        assertEquals(5, count);
    }

    @Test
    void sendWelcomeEmail_success() {
        notificationService.sendWelcomeEmail("welcome@example.com", "John");

        verify(emailProducer, times(1)).queueEmail(any());
    }

    @Test
    void sendNotification_withoutTargetEmail_skipsEmailButSendsWebSocket() {
        mockNotification.setTargetEmail(" ");
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);
        when(notificationRepository.countByUserIdAndIsRead(10, false)).thenReturn(1);

        notificationService.sendNotification(mockNotification);

        verify(emailProducer, never()).queueEmail(any());
        verify(notificationWebSocketController).sendNotificationToUser(10, mockNotification, 1);
    }

    @Test
    void markAsRead_throwsWhenNotificationMissing() {
        when(notificationRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.markAsRead(99));
    }

    @Test
    void markAllRead_success() {
        notificationService.markAllRead(10);

        verify(notificationRepository).markAllAsReadByUserId(10);
        verify(notificationWebSocketController).sendUnreadCountUpdate(10, 0);
    }

    @Test
    void getByUser_success() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10)).thenReturn(List.of(mockNotification));

        List<Notification> notifications = notificationService.getByUser(10);

        assertEquals(1, notifications.size());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(10);
    }

    @Test
    void getNotification_success() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(mockNotification));

        Notification notification = notificationService.getNotification(1);

        assertEquals(1, notification.getNotificationId());
    }

    @Test
    void getNotification_throwsWhenMissing() {
        when(notificationRepository.findById(7)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.getNotification(7));
    }

    @Test
    void deleteNotification_success() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(mockNotification));
        when(notificationRepository.countByUserIdAndIsRead(10, false)).thenReturn(0);

        notificationService.deleteNotification(1);

        verify(notificationRepository).deleteByNotificationId(1);
        verify(notificationWebSocketController).sendUnreadCountUpdate(10, 0);
    }

    @Test
    void deleteNotification_throwsWhenMissing() {
        when(notificationRepository.findById(5)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.deleteNotification(5));
    }

    @Test
    void getAll_success() {
        when(notificationRepository.findAll()).thenReturn(List.of(mockNotification));

        assertEquals(1, notificationService.getAll().size());
    }

    @Test
    void sendEmailAlert_skipsInvalidEmail() {
        notificationService.sendEmailAlert("bad-email", "Subject", "Body");

        verify(emailProducer, never()).queueEmail(any());
    }

    @Test
    void sendEmailAlert_queuesTrimmedValidEmail() {
        notificationService.sendEmailAlert(" user@example.com ", "Subject", "Body");

        var captor = forClass(EmailRequest.class);
        verify(emailProducer).queueEmail(captor.capture());
        assertEquals("user@example.com", captor.getValue().getTo());
        assertEquals("generic", captor.getValue().getTemplateName());
    }

    @Test
    void sendOtpEmail_success() {
        notificationService.sendOtpEmail("otp@example.com", "123456");

        verify(emailProducer).queueEmail(any(EmailRequest.class));
    }

    @Test
    void sendPurchaseConfirmation_success() {
        notificationService.sendPurchaseConfirmation(
                "buyer@example.com",
                "Buyer",
                "Spring Boot",
                "Instructor",
                "499",
                "ORD-1",
                null
        );

        var captor = forClass(EmailRequest.class);
        verify(emailProducer).queueEmail(captor.capture());
        assertEquals("purchase", captor.getValue().getTemplateName());
    }

    @Test
    void sendPasswordResetEmail_success() {
        notificationService.sendPasswordResetEmail("reset@example.com", "http://localhost/reset");

        verify(emailProducer).queueEmail(any(EmailRequest.class));
    }

    @Test
    void sendCertificateEmail_defaultsTemplateName() {
        EmailRequest request = EmailRequest.builder()
                .to("cert@example.com")
                .subject("Certificate")
                .build();

        notificationService.sendCertificateEmail(request);

        assertEquals("certificate", request.getTemplateName());
        verify(emailProducer).queueEmail(request);
    }

    @Test
    void sendCertificateEmail_stripsMailPrefix() {
        EmailRequest request = EmailRequest.builder()
                .to("cert@example.com")
                .subject("Certificate")
                .templateName("mail/certificate")
                .build();

        notificationService.sendCertificateEmail(request);

        assertEquals("certificate", request.getTemplateName());
        verify(emailProducer).queueEmail(request);
    }
}
