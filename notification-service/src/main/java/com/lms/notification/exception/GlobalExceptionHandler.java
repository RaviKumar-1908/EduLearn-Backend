package com.lms.notification.exception;

import com.lms.notification.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler – centralized exception handling for the Notification-Service.
 *
 * <p>{@code @RestControllerAdvice} intercepts exceptions thrown from any {@code @RestController}
 * and converts them into a uniform {@link ApiResponse} payload so the API Gateway
 * and downstream clients always receive a consistent error structure.
 *
 * @author LMS Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Domain Exceptions ────────────────────────────────────────────────────

    /**
     * Handles 404 – Notification not found.
     *
     * @param ex the thrown exception
     * @return 404 ApiResponse with the error message
     */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotificationNotFound(
            NotificationNotFoundException ex) {
        log.warn("NotificationNotFoundException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    // ── Validation Exceptions ─────────────────────────────────────────────────

    /**
     * Handles Bean Validation failures on {@code @RequestBody} annotated parameters.
     * Returns a 400 with a map of field → error message pairs.
     *
     * @param ex the validation exception
     * @return 400 ApiResponse with field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed: {}", errors);
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .statusCode(400)
                .message("Validation failed – check the 'data' field for details")
                .data(errors)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles constraint violations on path variables / request params.
     *
     * @param ex the constraint violation exception
     * @return 400 ApiResponse
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {
        log.warn("ConstraintViolationException: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    /**
     * Handles malformed JSON request bodies.
     *
     * @param ex the parse exception
     * @return 400 ApiResponse
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(
            HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, "Malformed JSON request body"));
    }

    /**
     * Handles type mismatches on path variable conversion (e.g., passing a String for an int).
     *
     * @param ex the type mismatch exception
     * @return 400 ApiResponse
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String msg = String.format("Parameter '%s' must be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        log.warn("Type mismatch: {}", msg);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, msg));
    }

    // ── Catch-All ────────────────────────────────────────────────────────────

    /**
     * Catch-all handler for any unexpected {@link Exception}.
     * Logs the full stack trace internally but returns a generic 500 to the client.
     *
     * @param ex the unhandled exception
     * @return 500 ApiResponse
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception in Notification-Service", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500,
                        "An unexpected error occurred. Please contact support."));
    }
}
