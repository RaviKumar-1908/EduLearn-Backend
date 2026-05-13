package com.lms.progress.exception;

import com.lms.progress.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised exception handling for all controllers.
 * Maps business exceptions to appropriate HTTP status codes
 * and wraps the error in a consistent {@link ApiResponse} envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ------------------------------------------------------------------ */
    /* 404 – resource not found                                             */
    /* ------------------------------------------------------------------ */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("ResourceNotFoundException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }


    /* ------------------------------------------------------------------ */
    /* 409 – duplicate certificate                                          */
    /* ------------------------------------------------------------------ */
    @ExceptionHandler(CertificateAlreadyIssuedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateCertificate(CertificateAlreadyIssuedException ex) {
        log.warn("CertificateAlreadyIssuedException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /* ------------------------------------------------------------------ */
    /* 422 – course not yet complete                                        */
    /* ------------------------------------------------------------------ */
    @ExceptionHandler(CourseNotCompletedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourseNotCompleted(CourseNotCompletedException ex) {
        log.warn("CourseNotCompletedException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /* ------------------------------------------------------------------ */
    /* 400 – bad request / illegal argument                                 */
    /* ------------------------------------------------------------------ */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /* ------------------------------------------------------------------ */
    /* 400 – missing parameters                                             */
    /* ------------------------------------------------------------------ */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        log.warn("MissingServletRequestParameterException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Missing required parameter: " + ex.getParameterName()));
    }

    /* ------------------------------------------------------------------ */
    /* 500 – unexpected errors                                              */
    /* ------------------------------------------------------------------ */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
