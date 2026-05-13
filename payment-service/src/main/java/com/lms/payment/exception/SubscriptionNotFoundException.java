package com.lms.payment.exception;

/** Thrown when no matching subscription record can be located. */
public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}
