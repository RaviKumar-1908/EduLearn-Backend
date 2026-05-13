package com.lms.payment.exception;

/** Thrown when a payment operation violates business rules (e.g., duplicate txn, invalid refund). */
public class InvalidPaymentException extends RuntimeException {
    public InvalidPaymentException(String message) {
        super(message);
    }
}
