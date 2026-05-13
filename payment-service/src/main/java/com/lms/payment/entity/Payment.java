package com.lms.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single payment transaction made by a student for a course.
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private int paymentId;

    @Column(name = "student_id", nullable = false)
    private int studentId;

    @Column(name = "course_id", nullable = false)
    private int courseId;

    @Column(nullable = false)
    private double amount;

    /**
     * Possible values: PENDING, SUCCESS, FAILED, REFUNDED
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * Payment mode: CARD, UPI, NET_BANKING, WALLET
     */
    @Column(nullable = false, length = 30)
    private String mode;

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(length = 10)
    private String currency;
}
