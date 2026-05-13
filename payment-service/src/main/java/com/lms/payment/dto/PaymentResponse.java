package com.lms.payment.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/** DTO returned to callers after a payment operation. */
@Data
@Builder
public class PaymentResponse {
    private int paymentId;
    private int studentId;
    private int courseId;
    private double amount;
    private String status;
    private String mode;
    private String transactionId;
    private LocalDateTime paidAt;
    private String currency;
    private String message;
}
