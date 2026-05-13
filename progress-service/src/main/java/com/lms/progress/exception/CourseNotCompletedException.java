package com.lms.progress.exception;

/** Thrown when a certificate is requested but the course is not yet 100% complete. */
public class CourseNotCompletedException extends RuntimeException {
    public CourseNotCompletedException(String message) {
        super(message);
    }
}
