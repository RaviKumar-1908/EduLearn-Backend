package com.lms.payment.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO for incoming payment initiation requests.
 * Regex patterns enforce strict input validation before anything hits the service layer.
 */
@Data
public class PaymentRequest {

    @Min(value = 1, message = "Student ID must be a positive integer")
    private int studentId;

    @Min(value = 1, message = "Course ID must be a positive integer")
    private int courseId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be zero or greater")
    private double amount;

    /**
     * Accepts: CARD, UPI, NET_BANKING, WALLET (case-insensitive)
     */
    @Pattern(regexp = "^$|^(CARD|UPI|NET_BANKING|WALLET|FREE|ONLINE)$",
             message = "Mode must be one of: CARD, UPI, NET_BANKING, WALLET, FREE, ONLINE")
    private String mode;

    /**
     * ISO 4217 currency code — exactly 3 uppercase letters (INR, USD, EUR …)
     */
    @Pattern(regexp = "^$|^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g. INR)")
    private String currency;

    /**
     * Transaction ID from the payment gateway — alphanumeric, 8-100 chars
     */
    @Pattern(regexp = "^$|^[A-Za-z0-9_-]{3,100}$",
             message = "Transaction ID must be 3-100 alphanumeric characters")
    private String transactionId;

    private String courseTitle;
}
