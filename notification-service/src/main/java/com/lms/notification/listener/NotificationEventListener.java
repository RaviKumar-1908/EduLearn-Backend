package com.lms.notification.listener;

import com.lms.notification.entity.Notification;
import com.lms.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NotificationEventListener – RabbitMQ consumer for all LMS platform events.
 *
 * <p>Each method listens on a dedicated queue and converts the incoming JSON message
 * (deserialized via Jackson by Spring AMQP) into a {@link Notification} entity,
 * then delegates to {@link NotificationService#sendNotification(Notification)}.
 *
 * <h3>Expected message payload (JSON)</h3>
 * <pre>
 * {
 *   "userId":          1,
 *   "title":           "Enrollment Confirmed",
 *   "message":         "You have been enrolled in Spring Boot Mastery",
 *   "relatedEntityId": 42,
 *   "relatedEntityType": "COURSE",
 *   "email":           "student@example.com"   // optional – triggers email alert
 * }
 * </pre>
 *
 * @author LMS Team
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public NotificationEventListener(NotificationService notificationService, org.springframework.web.client.RestTemplate restTemplate) {
        this.notificationService = notificationService;
        this.restTemplate = restTemplate;
    }

    // ── Enrollment Events ─────────────────────────────────────────────────────

    /**
     * Consumes enrollment confirmation events from {@code enrollment.notification.queue}.
     * Triggered when a student successfully enrolls in a course.
     *
     * @param payload deserialized event map
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.enrollment}")
    public void onEnrollmentEvent(@Payload Map<String, Object> payload) {
        log.info("[RabbitMQ] 📥 Received ENROLLMENT event: {}", payload);
        try {
            String typeFromPayload = toString(payload.get("type"), "ENROLLMENT");
            Notification notification = buildNotification(payload, typeFromPayload);
            if (!hasRecipient(notification, payload)) {
                log.warn("[RabbitMQ] ⚠ Skipping ENROLLMENT event - No recipient found");
                return;
            }
            notificationService.sendNotification(notification);
            sendEmailIfPresent(payload, notification);
            
            // Notify Admin
            notificationService.notifyAdmins(
                "📚 New Enrollment",
                toString(payload.get("fullName"), "A student") + " enrolled in '" + toString(payload.get("courseTitle"), "a course") + "'.",
                "ENROLLMENT",
                notification.getRelatedEntityId(),
                "COURSE"
            );
            
            log.info("[RabbitMQ] ✅ ENROLLMENT event ({}) processed successfully", typeFromPayload);
        } catch (Exception e) {
            log.error("[RabbitMQ] ❌ Failed to process ENROLLMENT event: {}", e.getMessage(), e);
        }
    }

    // ── Payment Events ────────────────────────────────────────────────────────

    /**
     * Consumes payment confirmation events from {@code payment.notification.queue}.
     * Triggered when a payment is successfully processed.
     *
     * @param payload deserialized event map
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.payment}")
    public void onPaymentEvent(@Payload Map<String, Object> payload) {
        log.info("[RabbitMQ] 📥 Received PAYMENT event: {}", payload);
        try {
            Notification notification = buildNotification(payload, "PAYMENT");
            if (!hasRecipient(notification, payload)) {
                log.warn("[RabbitMQ] ⚠ Skipping PAYMENT event - No recipient found");
                return;
            }
            notificationService.sendNotification(notification);
            
            // Notify Admin
            notificationService.notifyAdmins(
                "💳 Payment Success: " + toString(payload.get("amount"), "0.0"),
                toString(payload.get("fullName"), "User") + " purchased " + toString(payload.get("courseName"), "a course"),
                "PAYMENT",
                notification.getRelatedEntityId(),
                "COURSE"
            );
            
            String email = toString(payload.get("email"), null);
            if (email != null) {
                log.info("[RabbitMQ] 📧 Sending purchase confirmation email to {}", email);
                String fullName = toString(payload.get("fullName"), "Student");
                String courseName = toString(payload.get("courseName"), "Course");
                String instructorName = toString(payload.get("instructorName"), "Instructor");
                String amount = toString(payload.get("amount"), "0.0");
                String orderId = toString(payload.get("transactionId"), "#LMS-ORDER");
                String thumb = toString(payload.get("thumbnailUrl"), null);
                
                notificationService.sendPurchaseConfirmation(email, fullName, courseName, instructorName, amount, orderId, thumb);
            }
            log.info("[RabbitMQ] ✅ PAYMENT event processed successfully");
        } catch (Exception e) {
            log.error("[RabbitMQ] ❌ Failed to process PAYMENT event: {}", e.getMessage(), e);
        }
    }

    // ── Quiz Events ───────────────────────────────────────────────────────────

    /**
     * Consumes quiz result events from {@code quiz.notification.queue}.
     * Triggered when quiz evaluation is complete.
     *
     * @param payload deserialized event map
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.quiz}")
    public void onQuizEvent(@Payload Map<String, Object> payload) {
        log.info("Received QUIZ_RESULT event from RabbitMQ: userId={}", payload.get("userId"));
        Notification notification = buildNotification(payload, "QUIZ_RESULT");
        if (!hasRecipient(notification, payload)) return;
        notificationService.sendNotification(notification);
        sendEmailIfPresent(payload, notification);
    }

    // ── Certificate Events ────────────────────────────────────────────────────

    /**
     * Consumes certificate issuance events from {@code certificate.notification.queue}.
     * Triggered when a certificate is generated for a completed course.
     *
     * @param payload deserialized event map
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.certificate}")
    public void onCertificateEvent(@Payload Map<String, Object> payload) {
        log.info("Received CERTIFICATE event from RabbitMQ: userId={}", payload.get("userId"));
        Notification notification = buildNotification(payload, "CERTIFICATE");
        if (!hasRecipient(notification, payload)) return;
        notificationService.sendNotification(notification);
        sendEmailIfPresent(payload, notification);

        // Notify Admin
        notificationService.notifyAdmins(
            "🎓 Certificate Earned",
            "A student has completed a course and earned a certificate.",
            "CERTIFICATE",
            notification.getRelatedEntityId(),
            "COURSE"
        );
    }

    // ── Course Publication Events ─────────────────────────────────────────────

    /**
     * Consumes course publication events from {@code course.notification.queue}.
     * Triggered when an instructor publishes a new course.
     *
     * @param payload deserialized event map
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.course}")
    public void onCourseEvent(@Payload Map<String, Object> payload) {
        String type = toString(payload.get("type"), "COURSE_EVENT");
        log.info("Received {} event from RabbitMQ", type);
        Notification notification = buildNotification(payload, type);
        if (!hasRecipient(notification, payload)) return;
        notificationService.sendNotification(notification);
        sendEmailIfPresent(payload, notification);

        // Notify Admin
        notificationService.notifyAdmins(
            "🚀 Course Published",
            "A new course (" + toString(payload.get("title"), "Course") + ") has been published.",
            "COURSE_PUBLISHED",
            notification.getRelatedEntityId(),
            "COURSE"
        );
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.auth}")
    public void onAuthEvent(@Payload Map<String, Object> payload) {
        String type = toString(payload.get("type"), "AUTH_EVENT");
        log.info("Received {} event from RabbitMQ: userId={}", type, payload.get("userId"));
        
        Notification notification = buildNotification(payload, type);
        if (!hasRecipient(notification, payload)) return;
        notificationService.sendNotification(notification);
        
        // Notify Admin for key auth events
        if ("AUTH_REGISTER".equalsIgnoreCase(type)) {
            notificationService.notifyAdmins(
                "👤 New User Registered",
                toString(payload.get("fullName"), "User") + " joined the platform.",
                "AUTH_REGISTER",
                notification.getUserId(),
                "USER"
            );
        } else if (type != null && type.startsWith("BUG_REPORT")) {
            notificationService.notifyAdmins(
                "🐞 Bug Reported",
                notification.getMessage(),
                "BUG_REPORT",
                notification.getRelatedEntityId(),
                "BUG"
            );
        }

        String email = toString(payload.get("email"), null);
        if (email != null) {
            if ("AUTH_REGISTER".equalsIgnoreCase(type)) {
                String fullName = toString(payload.get("fullName"), "User");
                notificationService.sendWelcomeEmail(email, fullName);
            } else if ("AUTH_FORGOT_PASSWORD".equalsIgnoreCase(type)) {
                String otp = toString(payload.get("otp"), null);
                if (otp != null) {
                    notificationService.sendOtpEmail(email, otp);
                } else {
                    String message = toString(payload.get("message"), "");
                    notificationService.sendEmailAlert(email, notification.getTitle(), message);
                }
            } else if ("AUTH_LOGIN".equalsIgnoreCase(type) || "AUTH_STATUS_CHANGE".equalsIgnoreCase(type) 
                        || "BUG_REPORT_CONFIRMATION".equalsIgnoreCase(type) || "BUG_REPORT_UPDATE".equalsIgnoreCase(type)
                        || "ADMIN_ALERT".equalsIgnoreCase(type)) {
                 notificationService.sendEmailAlert(email, notification.getTitle(), notification.getMessage());
            }
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.discussion}")
    public void onDiscussionEvent(@Payload Map<String, Object> payload) {
        String type = toString(payload.get("type"), "DISCUSSION_EVENT");
        log.info("Received {} event from RabbitMQ: userId={}", type, payload.get("userId"));
        Notification notification = buildNotification(payload, type);
        if (!hasRecipient(notification, payload)) return;
        notificationService.sendNotification(notification);
        sendEmailIfPresent(payload, notification);
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.lesson}")
    public void onLessonEvent(@Payload Map<String, Object> payload) {
        String type = toString(payload.get("type"), "LESSON_PUBLISHED");
        log.info("[RabbitMQ] ▶ Received {} event | payload={}", type, payload);
        try {
            int courseId = toInt(payload.get("courseId"));
            int instructorId = toInt(payload.get("instructorId"));
            int userIdFromPayload = toInt(payload.get("userId"));

            // Case A: Broadcast to all enrolled students
            if (userIdFromPayload == 0 && courseId > 0 && "LESSON_PUBLISHED".equals(type)) {
                log.info("[RabbitMQ] 📢 Broadcasting lesson publication to all students in course {}", courseId);
                try {
                    String enrollmentServiceUrl = "http://enrollment-service/api/enrollment/course/" + courseId;
                    java.util.List<java.util.Map<String, Object>> enrollments = restTemplate.getForObject(enrollmentServiceUrl, java.util.List.class);
                    
                    if (enrollments != null) {
                        for (java.util.Map<String, Object> enrollment : enrollments) {
                            int studentId = toInt(enrollment.get("studentId"));
                            if (studentId > 0) {
                                Notification studentNotif = buildNotification(payload, type);
                                studentNotif.setUserId(studentId);
                                notificationService.sendNotification(studentNotif);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("[RabbitMQ] ✘ Failed to fetch students from enrollment-service: {}", e.getMessage());
                }
            }

            // Case B: Direct notification (e.g. for Instructor confirmation)
            Notification notification = buildNotification(payload, type);
            if (notification.getUserId() > 0) {
                notificationService.sendNotification(notification);
                sendEmailIfPresent(payload, notification);
            }
            
            log.info("[RabbitMQ] ✔ {} event processed successfully", type);
        } catch (Exception e) {
            log.error("[RabbitMQ] ✘ Failed to process {} event | payload={} | error={}",
                    type, payload, e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "progress.notification.queue")
    public void onProgressEvent(@Payload Map<String, Object> payload) {
        String type = toString(payload.get("type"), "PROGRESS_UPDATED");
        log.info("[RabbitMQ] ▶ Received {} event | payload={}", type, payload);
        try {
            Notification notification = buildNotification(payload, type);
            if (!hasRecipient(notification, payload)) return;
            notificationService.sendNotification(notification);
            // Progress updates usually don't need emails, but we log the receipt
            log.info("[RabbitMQ] ✔ {} event processed | userId={}", type, notification.getUserId());
        } catch (Exception e) {
            log.error("[RabbitMQ] ✘ Failed to process {} event | payload={} | error={}",
                    type, payload, e.getMessage(), e);
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Constructs a {@link Notification} from the raw event payload map.
     *
     * @param payload event data
     * @param type    notification type constant
     * @return populated (not yet persisted) notification
     */
    private Notification buildNotification(Map<String, Object> payload, String type) {
        int userId = 0;
        if (payload.get("userId") != null) userId = toInt(payload.get("userId"));
        else if (payload.get("studentId") != null) userId = toInt(payload.get("studentId"));
        else if (payload.get("instructorId") != null) userId = toInt(payload.get("instructorId"));
        else if (payload.get("id") != null) userId = toInt(payload.get("id"));

        String title        = toString(payload.get("title"),   "LMS Notification");
        String message      = toString(payload.get("message"), "You have a new notification");
        int relatedEntityId = toInt(payload.get("relatedEntityId"));
        if (relatedEntityId == 0) relatedEntityId = toInt(payload.get("courseId"));
        if (relatedEntityId == 0) relatedEntityId = toInt(payload.get("lessonId"));

        String entityType   = toString(payload.get("relatedEntityType"), type);

        return Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(entityType)
                .build();
    }

    /**
     * Dispatches an email alert if the payload contains a non-blank {@code email} field.
     *
     * @param payload      event data (may contain "email" key)
     * @param notification already-persisted notification for subject/body
     */
    private void sendEmailIfPresent(Map<String, Object> payload, Notification notification) {
        String email = toString(payload.get("email"), null);
        
        if ((email == null || email.isBlank()) && notification.getUserId() > 0) {
            log.debug("Email missing in payload for userId={}, attempting to fetch from auth-service", notification.getUserId());
            try {
                String authServiceUrl = "http://auth-service/auth/profile/" + notification.getUserId();
                java.util.Map<?, ?> userProfile = restTemplate.getForObject(authServiceUrl, java.util.Map.class);
                if (userProfile != null && userProfile.get("email") != null) {
                    email = userProfile.get("email").toString();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch email for userId={} from auth-service: {}", notification.getUserId(), e.getMessage());
            }
        }

        if (email != null && !email.isBlank()) {
            notificationService.sendEmailAlert(
                    email,
                    notification.getTitle(),
                    notification.getMessage()
            );
        }
    }

    private boolean hasRecipient(Notification notification, Map<String, Object> payload) {
        if (notification.getUserId() > 0) {
            return true;
        }
        log.warn("Skipping notification event without target user. type={}, payload={}", notification.getType(), payload);
        return false;
    }

    /** Safe int extractor from an Object (handles Integer and String types). */
    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer i) return i;
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return 0; }
    }

    /** Safe String extractor with fallback default. */
    private String toString(Object value, String defaultValue) {
        return (value != null && !value.toString().isBlank()) ? value.toString() : defaultValue;
    }
}
