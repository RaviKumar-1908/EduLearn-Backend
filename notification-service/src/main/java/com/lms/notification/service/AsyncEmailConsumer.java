package com.lms.notification.service;

import com.lms.notification.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailConsumer {

    private final MailService mailService;

    @RabbitListener(queues = "${app.rabbitmq.queue.email.general}")
    public void consumeGeneralEmail(EmailRequest request) {
        log.info("[Email-Consumer] 📥 Received GENERAL email request for: {}", request.getTo());
        processEmail(request);
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.email.auth}")
    public void consumeAuthEmail(EmailRequest request) {
        log.info("[Email-Consumer] 📥 Received AUTH email request for: {}", request.getTo());
        processEmail(request);
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.email.purchase}")
    public void consumePurchaseEmail(EmailRequest request) {
        log.info("[Email-Consumer] 📥 Received PURCHASE email request for: {}", request.getTo());
        processEmail(request);
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.email.instructor}")
    public void consumeInstructorEmail(EmailRequest request) {
        log.info("[Email-Consumer] 📥 Received INSTRUCTOR email request for: {}", request.getTo());
        processEmail(request);
    }

    private void processEmail(EmailRequest request) {
        try {
            log.info("[Email-Consumer] 📨 Attempting to send email to: {} | Template: {}", request.getTo(), request.getTemplateName());
            mailService.sendEmail(request);
            log.info("[Email-Consumer] ✅ Email dispatched successfully to: {}", request.getTo());
        } catch (Exception e) {
            log.error("[Email-Consumer] ❌ FAILED to send email to: {} | Error: {}", request.getTo(), e.getMessage());
        }
    }
}
