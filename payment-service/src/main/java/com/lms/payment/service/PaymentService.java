package com.lms.payment.service;

import com.lms.payment.entity.Payment;
import com.lms.payment.entity.Subscription;

import java.util.List;
import java.util.Optional;

/**
 * Contract for all payment and subscription operations.
 * The controller depends only on this interface — never on the concrete impl.
 * This decoupling makes unit-testing straightforward (Mockito can stub this easily).
 */
public interface PaymentService {

    /** Persists a new payment record and triggers the payment-event to RabbitMQ. */
    Payment processPayment(Payment payment);
    Payment processPaymentWithMetadata(Payment payment, java.util.Map<String, Object> metadata);

    /** Returns every payment record for the given student. */
    List<Payment> getPaymentsByStudent(int studentId);

    /** Returns every payment record for the given course. */
    List<Payment> getPaymentsByCourse(int courseId);

    /**
     * Creates a new subscription for the student under the specified plan.
     * @param studentId  the ID of the student
     * @param plan       FREE | MONTHLY | ANNUAL
     */
    Subscription subscribe(int studentId, String plan);

    /** Marks the student's active subscription as CANCELLED. */
    void cancelSubscription(int studentId);

    /** Extends the active subscription by its plan duration and returns the updated record. */
    Subscription renewSubscription(int studentId);

    /** Fetches the student's subscription (any status). */
    Optional<Subscription> getSubscriptionByStudent(int studentId);

    /** Returns true only when the student has an ACTIVE subscription that hasn't expired. */
    boolean isSubscriptionActive(int studentId);

    /** Marks the payment as REFUNDED and publishes a refund event. */
    void refundPayment(int paymentId);

    /** Creates a Razorpay order. */
    String createRazorpayOrder(double amount);

    /** Verifies Razorpay signature. */
    boolean verifyRazorpayPayment(String orderId, String paymentId, String signature);

    /** Returns financial statistics for admin dashboard. */
    java.util.Map<String, Object> getAdminStats();
}
