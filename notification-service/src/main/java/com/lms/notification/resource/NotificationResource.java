package com.lms.notification.resource;

import com.lms.notification.dto.ApiResponse;
import com.lms.notification.dto.BulkNotificationRequest;
import com.lms.notification.dto.NotificationRequest;
import com.lms.notification.entity.Notification;
import com.lms.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * NotificationResource – REST controller exposing all Notification-Service endpoints.
 *
 * <p>All routes are prefixed with {@code /notifications} and accessed exclusively
 * through the API Gateway on port 8000.
 *
 * <p>JWT validation is handled upstream by the Gateway / {@link com.lms.notification.security.JwtAuthFilter}.
 *
 * @author LMS Team
 */
@RestController
@RequestMapping({"/api/notification", "/api/notifications", "/notifications"})
@Validated
@Tag(name = "Notification API", description = "Endpoints for dispatching and managing LMS notifications")
public class NotificationResource {

    private static final Logger log = LoggerFactory.getLogger(NotificationResource.class);

    private final NotificationService notifService;

    /**
     * Constructor injection – preferred over {@code @Autowired} field injection.
     *
     * @param notifService notification service implementation
     */
    public NotificationResource(NotificationService notifService) {
        this.notifService = notifService;
    }

    // ── Send Single Notification ─────────────────────────────────────────────

    /**
     * POST /notifications/send
     * Dispatches a single notification to one user.
     */
    @PostMapping("/send")
    @Operation(summary = "Send a notification", description = "Persists and dispatches a single notification")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Notification sent"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<ApiResponse<Void>> sendNotification(
            @Valid @RequestBody NotificationRequest request) {

        log.info("POST /notifications/send – userId={}, type={}", request.getUserId(), request.getType());

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedEntityId(request.getRelatedEntityId())
                .relatedEntityType(request.getRelatedEntityType())
                .targetEmail(request.getTargetEmail())
                .build();

        notifService.sendNotification(notification);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Notification sent successfully", null));
    }

    // ── Send Bulk Notification ───────────────────────────────────────────────

    /**
     * POST /notifications/bulk
     * Dispatches the same notification to multiple users (e.g., course publication).
     */
    @PostMapping("/bulk")
    @Operation(summary = "Send bulk notifications", description = "Dispatches identical notifications to a list of users")
    public ResponseEntity<ApiResponse<Void>> sendBulkNotification(
            @Valid @RequestBody BulkNotificationRequest request) {

        log.info("POST /notifications/bulk – {} recipients, type={}", request.getUserIds().size(), request.getType());

        notifService.sendBulkNotification(
                request.getUserIds(),
                request.getTitle(),
                request.getMessage(),
                request.getType(),
                request.getRelatedEntityId(),
                request.getRelatedEntityType()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201,
                        "Bulk notifications sent to " + request.getUserIds().size() + " users", null));
    }

    // ── Get By User ──────────────────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notifications by user", description = "Returns all notifications for a user (Redis cached)")
    public ResponseEntity<ApiResponse<List<Notification>>> getByUser(
            @Parameter(description = "Target user ID", required = true)
            @PathVariable @Positive(message = "userId must be positive") int userId,
            Authentication authentication) {

        assertUserAccess(userId, authentication);

        log.debug("GET /notifications/user/{}", userId);
        List<Notification> notifications = notifService.getByUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", notifications));
    }

    @GetMapping("/my")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user notifications", description = "Returns the authenticated user's notifications")
    public ResponseEntity<ApiResponse<List<Notification>>> getMine(Authentication authentication) {
        int userId = requireAuthenticatedUserId(authentication);

        log.debug("GET /notifications/my -> user {}", userId);
        List<Notification> notifications = notifService.getByUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", notifications));
    }

    // ── Mark Single as Read ──────────────────────────────────────────────────

    /**
     * PUT /notifications/{notificationId}/read
     * Marks one notification as read.
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable @Positive(message = "notificationId must be positive") int notificationId,
            Authentication authentication) {

        Notification notification = notifService.getNotification(notificationId);
        assertNotificationAccess(notification, authentication);

        log.debug("PUT /notifications/{}/read", notificationId);
        notifService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    // ── Mark All as Read ─────────────────────────────────────────────────────

    /**
     * PUT /notifications/user/{userId}/read-all
     * Marks every unread notification for a user as read in one batch update.
     */
    @PutMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all notifications as read for a user")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @PathVariable @Positive(message = "userId must be positive") int userId,
            Authentication authentication) {

        assertUserAccess(userId, authentication);

        log.debug("PUT /notifications/user/{}/read-all", userId);
        notifService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @PutMapping("/my/read-all")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read for the current user")
    public ResponseEntity<ApiResponse<Void>> markMyNotificationsRead(Authentication authentication) {
        int userId = requireAuthenticatedUserId(authentication);

        log.debug("PUT /notifications/my/read-all -> user {}", userId);
        notifService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    // ── Unread Count ─────────────────────────────────────────────────────────

    @GetMapping("/user/{userId}/unread-count")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notification count", description = "Used to populate the notification bell badge")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(
            @PathVariable @Positive(message = "userId must be positive") int userId,
            Authentication authentication) {

        assertUserAccess(userId, authentication);

        log.debug("GET /notifications/user/{}/unread-count", userId);
        int count = notifService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched", count));
    }

    @GetMapping("/my/unread-count")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user unread notification count")
    public ResponseEntity<ApiResponse<Integer>> getMyUnreadCount(Authentication authentication) {
        int userId = requireAuthenticatedUserId(authentication);

        log.debug("GET /notifications/my/unread-count -> user {}", userId);
        int count = notifService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched", count));
    }

    // ── Delete Notification ──────────────────────────────────────────────────

    /**
     * DELETE /notifications/{notificationId}
     * Hard-deletes a single notification by its ID.
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable @Positive(message = "notificationId must be positive") int notificationId,
            Authentication authentication) {

        Notification notification = notifService.getNotification(notificationId);
        assertNotificationAccess(notification, authentication);

        log.info("DELETE /notifications/{}", notificationId);
        notifService.deleteNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Delete all notifications for a user")
    public ResponseEntity<ApiResponse<Void>> deleteAll(
            @PathVariable @Positive(message = "userId must be positive") int userId,
            Authentication authentication) {

        assertUserAccess(userId, authentication);

        log.info("DELETE /notifications/user/{}", userId);
        notifService.deleteAllForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications deleted successfully", null));
    }

    @DeleteMapping("/my")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete all notifications for current user")
    public ResponseEntity<ApiResponse<Void>> deleteMyNotifications(Authentication authentication) {
        int userId = requireAuthenticatedUserId(authentication);

        log.info("DELETE /notifications/my -> user {}", userId);
        notifService.deleteAllForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications deleted successfully", null));
    }

    // ── Get All (Admin) ──────────────────────────────────────────────────────

    /**
     * GET /notifications/all
     * Returns all notifications across all users – admin-only endpoint.
     */
    @GetMapping("/all")
    @Operation(summary = "Get all notifications (admin)", description = "Returns all notifications – no caching")
    public ResponseEntity<ApiResponse<List<Notification>>> getAll() {
        log.debug("GET /notifications/all (admin)");
        List<Notification> all = notifService.getAll();
        return ResponseEntity.ok(ApiResponse.success("All notifications fetched", all));
    }

    private void assertNotificationAccess(Notification notification, Authentication authentication) {
        assertUserAccess(notification.getUserId(), authentication);
    }

    private int requireAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Integer authenticatedUserId = resolveAuthenticatedUserId(authentication);
        if (authenticatedUserId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user id is missing");
        }

        return authenticatedUserId;
    }

    private void assertUserAccess(int requestedUserId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (isAdmin(authentication)) {
            return;
        }

        Integer authenticatedUserId = resolveAuthenticatedUserId(authentication);
        if (authenticatedUserId == null || authenticatedUserId != requestedUserId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access another user's notifications");
        }
    }

    @SuppressWarnings("unchecked")
    private Integer resolveAuthenticatedUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> map) {
            Object userId = map.get("userId");
            if (userId instanceof Number number) {
                return number.intValue();
            }
            if (userId != null) {
                try {
                    return Integer.parseInt(userId.toString());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
