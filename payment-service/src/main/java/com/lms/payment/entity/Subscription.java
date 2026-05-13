package com.lms.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Represents a student's subscription plan in the LMS.
 */
@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private int subscriptionId;

    @Column(name = "student_id", nullable = false)
    private int studentId;

    /**
     * Plan tier: FREE, MONTHLY, ANNUAL
     */
    @Column(nullable = false, length = 20)
    private String plan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Possible values: ACTIVE, EXPIRED, CANCELLED
     */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "amount_paid")
    private double amountPaid;

    @Column(name = "auto_renew")
    private boolean autoRenew;
}
