package com.lms.payment.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

/** Uniform error body returned for all exceptions. */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}
