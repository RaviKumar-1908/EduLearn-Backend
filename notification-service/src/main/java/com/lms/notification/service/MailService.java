package com.lms.notification.service;

import com.lms.notification.dto.EmailRequest;
import com.lms.notification.entity.EmailLog;
import com.lms.notification.repository.EmailLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;

    @Value("${spring.mail.username:noreply@lms.com}")
    private String fromEmail;

    public void sendEmail(EmailRequest request) {
        log.info("Preparing to send email to {} with template {}", request.getTo(), request.getTemplateName());

        String templateName = normalizeTemplateName(request.getTemplateName());
        String attachmentName = (request.getAttachmentName() != null && !request.getAttachmentName().isBlank())
                ? request.getAttachmentName()
                : "Certificate.pdf";
        
        EmailLog emailLog = EmailLog.builder()
                .recipient(request.getTo())
                .subject(request.getSubject())
                .template(templateName)
                .status(EmailLog.EmailStatus.PENDING)
                .retryCount(0)
                .build();
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            Context context = new Context();
            if (request.getTemplateModel() != null) {
                context.setVariables(request.getTemplateModel());
            }
            
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(htmlContent, true);
            
            // Handle Attachment if present
            if (request.getAttachmentBase64() != null && !request.getAttachmentBase64().isEmpty()) {
                byte[] decodedBytes = Base64.getDecoder().decode(request.getAttachmentBase64());
                org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(decodedBytes);
                helper.addAttachment(attachmentName, resource);
            }
            
            mailSender.send(message);
            
            emailLog.setStatus(EmailLog.EmailStatus.SENT);
            emailLog.setSentAt(LocalDateTime.now());
            log.info("Email sent successfully to {}", request.getTo());
            
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", request.getTo(), e);
            emailLog.setStatus(EmailLog.EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        } finally {
            emailLogRepository.save(emailLog);
        }
    }

    private String normalizeTemplateName(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return "welcome";
        }
        if (templateName.startsWith("mail/")) {
            return templateName.substring("mail/".length());
        }
        return templateName;
    }
}
