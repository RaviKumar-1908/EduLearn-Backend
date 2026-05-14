package com.lms.notification.service.impl;

import com.lms.notification.entity.Notification;
import com.lms.notification.exception.NotificationNotFoundException;
import com.lms.notification.repository.NotificationRepository;
import com.lms.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.lms.notification.controller.NotificationWebSocketController;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.web.client.RestTemplate;

/**
 * NotificationServiceImpl – concrete implementation of {@link NotificationService}.
 *
 * <p>Key cross-cutting concerns handled here:
 * <ul>
 *   <li><b>SLF4J Logging</b> – every operation is logged at DEBUG (happy path) or WARN/ERROR (failures)</li>
 *   <li><b>Redis Caching</b> – {@code getByUser} is cached; cache is evicted on any mutating operation</li>
 *   <li><b>Email Regex Validation</b> – the {@code toEmail} parameter is validated before dispatching</li>
 *   <li><b>Transactional</b> – write operations are wrapped in DB transactions</li>
 * </ul>
 *
 * @author LMS Team
 */
@Service
@Transactional(readOnly = true)   // default read-only; mutating methods override with @Transactional
public class NotificationServiceImpl implements NotificationService {

    // ── Logger (SLF4J + Logback) ─────────────────────────────────────────────
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    // ── Email Regex – RFC-5322 simplified, covers common valid addresses ──────
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    // ── Redis Cache Name ─────────────────────────────────────────────────────
    private static final String CACHE_NOTIFICATIONS = "notifications";

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final NotificationRepository notificationRepository;
    private final com.lms.notification.service.AsyncEmailProducer emailProducer;
    private final NotificationWebSocketController notificationWebSocketController;
    private final RestTemplate restTemplate;

    @Value("${spring.mail.username:noreply@lms.com}")
    private String fromEmail;

    /**
     * Constructor injection – preferred over field injection for testability.
     *
     * @param notificationRepository JPA repository
     * @param emailSender            Spring Mail sender
     */
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   com.lms.notification.service.AsyncEmailProducer emailProducer,
                                   NotificationWebSocketController notificationWebSocketController,
                                   RestTemplate restTemplate) {
        this.notificationRepository = notificationRepository;
        this.emailProducer = emailProducer;
        this.notificationWebSocketController = notificationWebSocketController;
        this.restTemplate = restTemplate;
    }

    // ── Single Notification ───────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Persists the notification and evicts the user's cached notification list
     * so the next {@code getByUser} call returns fresh data.
     */
    @Override
    @Transactional
    // @CacheEvict(value = CACHE_NOTIFICATIONS, key = "#notification.userId")
    public void sendNotification(Notification notification) {
        log.debug("Sending notification to userId={} type={}", notification.getUserId(), notification.getType());

        Notification saved = notificationRepository.save(notification);
        
        // Auto-send email if email address is provided
        if (saved.getTargetEmail() != null && !saved.getTargetEmail().isBlank()) {
            sendEmailAlert(saved.getTargetEmail(), saved.getTitle(), saved.getMessage());
        }

        log.info("Notification persisted: notificationId={}, userId={}, type={}",
            saved.getNotificationId(), saved.getUserId(), saved.getType());

        // Send real-time notification via WebSocket
        log.info("[WebSocket] 🛰 Dispatching notification to user channel: /topic/notifications/{}", saved.getUserId());
        notificationWebSocketController.sendNotificationToUser(saved.getUserId(), saved, getUnreadCount(saved.getUserId()));
    }

    // ── Bulk Notification ─────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Iterates over each userId, builds a {@link Notification}, and delegates to
     * {@link #sendNotification(Notification)}. Cache eviction happens per-user
     * via the delegated method.
     */
    @Override
    @Transactional
    public void sendBulkNotification(List<Integer> userIds,
                                     String title,
                                     String message,
                                     String type,
                                     int relatedEntityId,
                                     String relatedEntityType) {
        log.info("Sending bulk notification to {} users, type={}", userIds.size(), type);

        userIds.forEach(userId -> {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .relatedEntityId(relatedEntityId)
                    .relatedEntityType(relatedEntityType)
                    .build();
            sendNotification(notification);
        });

        log.info("Bulk notification dispatch complete for {} users", userIds.size());
    }

    // ── Read-State Management ─────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Loads the notification, flips {@code isRead}, saves, and evicts cache.
     *
     * @throws NotificationNotFoundException if notificationId not found
     */
    @Override
    @Transactional
    // @CacheEvict(value = CACHE_NOTIFICATIONS, allEntries = true)
    public void markAsRead(int notificationId) {
        log.debug("markAsRead called for notificationId={}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        notification.setRead(true);
        notificationRepository.save(notification);

        log.info("Notification {} marked as read for userId={}", notificationId, notification.getUserId());
        notificationWebSocketController.sendUnreadCountUpdate(notification.getUserId(), getUnreadCount(notification.getUserId()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Issues a single bulk UPDATE query via the repository custom method.
     */
    @Override
    @Transactional
    // @CacheEvict(value = CACHE_NOTIFICATIONS, key = "#userId")
    public void markAllRead(int userId) {
        log.debug("markAllRead called for userId={}", userId);
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("All notifications marked as read for userId={}", userId);
        notificationWebSocketController.sendUnreadCountUpdate(userId, 0);
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Result is cached in Redis by {@code userId}. Subsequent calls within the TTL
     * window bypass the database entirely.
     */
    @Override
    // @Cacheable(value = CACHE_NOTIFICATIONS, key = "#userId")
    public List<Notification> getByUser(int userId) {
        log.debug("getByUser (cache miss) for userId={}", userId);
        List<Notification> results = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        log.debug("Fetched {} notifications for userId={}", results.size(), userId);
        return results;
    }

    @Override
    public Notification getNotification(int notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Counts unread notifications. Not cached individually since badge counts
     * must be real-time accurate; the underlying query is an indexed COUNT.
     */
    @Override
    public int getUnreadCount(int userId) {
        int count = notificationRepository.countByUserIdAndIsRead(userId, false);
        log.debug("Unread count for userId={}: {}", userId, count);
        return count;
    }

    // ── Deletion ──────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Validates existence first, then hard-deletes. Evicts the user's cache entry.
     *
     * @throws NotificationNotFoundException if notificationId not found
     */
    @Override
    @Transactional
    // @CacheEvict(value = CACHE_NOTIFICATIONS, allEntries = true)
    public void deleteNotification(int notificationId) {
        log.debug("deleteNotification called for notificationId={}", notificationId);

        // Validate existence before deletion so we can evict the right userId cache
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        notificationRepository.deleteByNotificationId(notificationId);
        log.info("Notification {} deleted (userId={})", notificationId, notification.getUserId());
        notificationWebSocketController.sendUnreadCountUpdate(notification.getUserId(), getUnreadCount(notification.getUserId()));
    }

    @Override
    @Transactional
    // @CacheEvict(value = CACHE_NOTIFICATIONS, key = "#userId")
    public void deleteAllForUser(int userId) {
        log.info("deleteAllForUser called for userId={}", userId);
        notificationRepository.deleteByUserId(userId);
        log.info("All notifications deleted for userId={}", userId);
        notificationWebSocketController.sendUnreadCountUpdate(userId, 0);
    }

    // ── Email Alert ───────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Validates the recipient address against {@link #EMAIL_PATTERN} before attempting
     * to dispatch via {@link JavaMailSender}. Failures are logged but do NOT propagate
     * so that a mail outage does not block notification persistence.
     *
     * @param toEmail recipient address – validated with Regex
     * @param subject email subject
     * @param body    email body text
     */
    @Override
    public void sendEmailAlert(String toEmail, String subject, String body) {
        // Legacy support – sends a general email using the welcome template as a fallback
        if (toEmail == null || !EMAIL_PATTERN.matcher(toEmail.trim()).matches()) {
            log.warn("sendEmailAlert skipped – invalid email address: '{}'", toEmail);
            return;
        }

        com.lms.notification.dto.EmailRequest request = com.lms.notification.dto.EmailRequest.builder()
                .to(toEmail.trim())
                .subject(subject)
                .templateName("generic")
                .templateModel(java.util.Map.of(
                    "name", "User", 
                    "title", subject,
                    "message", body, 
                    "dashboardUrl", "http://localhost:5173/student/dashboard"
                ))
                .build();
        
        emailProducer.queueEmail(request);
        log.info("Email alert queued for '{}' – subject: '{}'", toEmail, subject);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Returns all notifications across all users. Admin-only – no caching to ensure
     * admin dashboards always see the latest state.
     */
    @Override
    public List<Notification> getAll() {
        log.debug("getAll notifications (admin)");
        return notificationRepository.findAll();
    }

    // --- Specialized Email Implementations ---

    @Override
    public void sendWelcomeEmail(String toEmail, String name) {
        log.info("Queueing welcome email for {}", toEmail);
        com.lms.notification.dto.EmailRequest request = com.lms.notification.dto.EmailRequest.builder()
                .to(toEmail)
                .subject("Welcome to our Learning Platform!")
                .templateName("welcome")
                .templateModel(java.util.Map.of(
                    "name", name,
                    "dashboardUrl", "http://localhost:5173/student/dashboard"
                ))
                .build();
        emailProducer.queueEmail(request);
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("Queueing OTP email for {}", toEmail);
        com.lms.notification.dto.EmailRequest request = com.lms.notification.dto.EmailRequest.builder()
                .to(toEmail)
                .subject("Your Verification Code")
                .templateName("otp")
                .templateModel(java.util.Map.of("otp", otp))
                .build();
        emailProducer.queueEmail(request);
    }

    @Override
    public void sendPurchaseConfirmation(String toEmail, String name, String courseName, String instructorName, String amount, String orderId, String thumbnailUrl) {
        log.info("Queueing purchase confirmation for {} | Course: {}", toEmail, courseName);
        com.lms.notification.dto.EmailRequest request = com.lms.notification.dto.EmailRequest.builder()
                .to(toEmail)
                .subject("Enrollment Confirmed: " + courseName)
                .templateName("purchase")
                .templateModel(java.util.Map.of(
                    "name", name,
                    "courseName", courseName,
                    "instructorName", instructorName,
                    "amount", amount,
                    "orderId", orderId,
                    "thumbnailUrl", thumbnailUrl != null ? thumbnailUrl : "https://via.placeholder.com/300x200",
                    "courseUrl", "http://localhost:5173/student/my-learning"
                ))
                .build();
        emailProducer.queueEmail(request);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        log.info("Queueing password reset email for {}", toEmail);
        com.lms.notification.dto.EmailRequest request = com.lms.notification.dto.EmailRequest.builder()
                .to(toEmail)
                .subject("Reset Your Password")
                .templateName("generic")
                .templateModel(java.util.Map.of(
                    "name", "User",
                    "title", "Password Reset Request",
                    "message", "We received a request to reset your password. Click the button below to proceed. This link will expire shortly.",
                    "dashboardUrl", resetUrl
                ))
                .build();
        emailProducer.queueEmail(request);
    }

    @Override
    @Async
    public void sendCertificateEmail(com.lms.notification.dto.EmailRequest request) {
        if (request.getTemplateName() == null || request.getTemplateName().isBlank()) {
            request.setTemplateName("certificate");
        } else if (request.getTemplateName().startsWith("mail/")) {
            request.setTemplateName(request.getTemplateName().substring("mail/".length()));
        }
        log.info("Queueing certificate email for {}", request.getTo());
        emailProducer.queueEmail(request);
    }

    @Override
    public void notifyAdmins(String title, String message, String type, int relatedEntityId, String relatedEntityType) {
        log.info("[Admin-Notify] Broadcasting to all admins | type={}", type);
        try {
            String authServiceUrl = "http://auth-service/auth/admin/ids";
            List<Integer> adminIds = restTemplate.getForObject(authServiceUrl, List.class);
            
            if (adminIds != null && !adminIds.isEmpty()) {
                log.info("[Admin-Notify] Found {} admins to notify", adminIds.size());
                // The adminIds list from RestTemplate might be List<Integer> but Jackson might deserialize as List<Integer> correctly.
                // We map them to ensure they are integers if needed, though getForObject with List.class usually works.
                java.util.List<Integer> targetIds = adminIds.stream()
                        .map(id -> ((Number) id).intValue())
                        .toList();
                
                sendBulkNotification(targetIds, title, message, type, relatedEntityId, relatedEntityType);
            } else {
                log.warn("[Admin-Notify] No admins found in the system!");
            }
        } catch (Exception e) {
            log.error("[Admin-Notify] Failed to fetch admin IDs from auth-service: {}", e.getMessage());
        }
    }
}
