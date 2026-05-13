package com.lms.progress.exception;

/** Thrown when a requested resource (certificate, progress record) does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
