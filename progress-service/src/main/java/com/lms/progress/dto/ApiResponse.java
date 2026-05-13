package com.lms.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic envelope for all API responses.
 *
 * <pre>
 * {
 *   "status":  "success" | "error",
 *   "message": "Human-readable description",
 *   "data":    { ... } | null,
 *   "timestamp": "2025-01-01T12:00:00"
 * }
 * </pre>
 *
 * @param <T> type of the data payload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private String        status;
    private String        message;
    private T             data;
    private LocalDateTime timestamp;

    // ------------------------------------------------------------------ //
    // Factory helpers                                                      //
    // ------------------------------------------------------------------ //

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null, LocalDateTime.now());
    }
}
