package com.lms.notification.service;

import com.lms.notification.dto.EmailRequest;
import com.lms.notification.entity.EmailLog;
import com.lms.notification.repository.EmailLogRepository;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private EmailLogRepository emailLogRepository;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender, templateEngine, emailLogRepository);
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@example.com");
    }

    @Test
    void sendEmail_successWithDefaultTemplateName() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("welcome"), any(Context.class))).thenReturn("<html>Hello</html>");

        EmailRequest request = EmailRequest.builder()
                .to("student@example.com")
                .subject("Welcome")
                .templateModel(Map.of("name", "Ravi"))
                .build();

        mailService.sendEmail(request);

        verify(mailSender).send(mimeMessage);
        ArgumentCaptor<EmailLog> logCaptor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository).save(logCaptor.capture());
        assertEquals(EmailLog.EmailStatus.SENT, logCaptor.getValue().getStatus());
        assertNotNull(logCaptor.getValue().getSentAt());
    }

    @Test
    void sendEmail_successWithAttachmentAndMailPrefixTemplate() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("certificate"), any(Context.class))).thenReturn("<html>Certificate</html>");

        EmailRequest request = EmailRequest.builder()
                .to("student@example.com")
                .subject("Certificate")
                .templateName("mail/certificate")
                .attachmentBase64(Base64.getEncoder().encodeToString("hello".getBytes()))
                .attachmentName("MyCert.pdf")
                .build();

        mailService.sendEmail(request);

        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(eq("certificate"), any(Context.class));
    }

    @Test
    void sendEmail_marksFailureWhenMessagingSetupFails() {
        MimeMessage mimeMessage = spy(new JavaMailSenderImpl().createMimeMessage());
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("welcome"), any(Context.class))).thenReturn("<html>Hello</html>");
        try {
            doThrow(new MessagingException("bad recipient"))
                    .when(mimeMessage).setRecipients(eq(Message.RecipientType.TO), any(String.class));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        EmailRequest request = EmailRequest.builder()
                .to("student@example.com")
                .subject("Welcome")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> mailService.sendEmail(request));
        assertNotNull(ex.getMessage());

        // Capture all EmailLog saves and check the last one for FAILED status
        ArgumentCaptor<EmailLog> logCaptor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, atLeastOnce()).save(logCaptor.capture());
        EmailLog finalLog = logCaptor.getAllValues().get(logCaptor.getAllValues().size() - 1);
        assertEquals(EmailLog.EmailStatus.FAILED, finalLog.getStatus());
        assertNotNull(finalLog.getErrorMessage());
    }
}
