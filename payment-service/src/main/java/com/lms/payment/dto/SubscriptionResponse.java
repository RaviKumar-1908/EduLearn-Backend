package com.lms.payment.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

/** DTO returned after subscription operations. */
@Data
@Builder
public class SubscriptionResponse {
    private int subscriptionId;
    private int studentId;
    private String plan;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private double amountPaid;
    private boolean autoRenew;
    private String message;
}
