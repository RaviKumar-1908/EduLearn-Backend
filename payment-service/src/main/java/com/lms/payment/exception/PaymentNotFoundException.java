package com.lms.payment.exception;

/**
 * Thrown when a payment record cannot be found in the database.
 * Extends RuntimeException so it doesn't force checked-exception boilerplate at call sites.
 */
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
