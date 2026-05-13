package com.lms.payment.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO for creating or renewing a subscription.
 */
@Data
public class SubscriptionRequest {

    @Min(value = 1, message = "Student ID must be a positive integer")
    private int studentId;

    /**
     * Plan must be one of the three supported tiers.
     */
    @NotBlank(message = "Plan is required")
    @Pattern(regexp = "^(FREE|MONTHLY|ANNUAL)$",
             message = "Plan must be FREE, MONTHLY, or ANNUAL")
    private String plan;

    private boolean autoRenew;
}
