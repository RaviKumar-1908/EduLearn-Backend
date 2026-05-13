package com.lms.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * ApiResponse – generic envelope for all REST responses.
 *
 * <p>Wraps the payload with a status flag, HTTP status code, message,
 * and timestamp so that clients always receive a consistent shape.
 *
 * @param <T> type of the response payload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    /** {@code true} when the operation succeeded, {@code false} otherwise. */
    private boolean success;

    /** HTTP status code mirrored in the body for client convenience. */
    private int statusCode;

    /** Human-readable result or error description. */
    private String message;

    /** Actual response payload; {@code null} for void operations. */
    private T data;

    /** Server-side timestamp at the point of response creation. */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Factory helpers ──────────────────────────────────────────────────────

    /**
     * Creates a success response with payload.
     *
     * @param message descriptive message
     * @param data    payload to wrap
     * @param <T>     payload type
     * @return success ApiResponse
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates a success response with a custom HTTP status code.
     *
     * @param statusCode HTTP code (e.g., 201 for CREATED)
     * @param message    descriptive message
     * @param data       payload
     * @param <T>        payload type
     * @return success ApiResponse
     */
    public static <T> ApiResponse<T> success(int statusCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates an error response without payload.
     *
     * @param statusCode HTTP error code
     * @param message    error description
     * @param <T>        payload type
     * @return error ApiResponse
     */
    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .build();
    }
}
