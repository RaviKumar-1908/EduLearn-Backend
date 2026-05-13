package com.lms.payment.resource;

import com.lms.payment.dto.PaymentRequest;
import com.lms.payment.dto.PaymentResponse;
import com.lms.payment.dto.SubscriptionRequest;
import com.lms.payment.dto.SubscriptionResponse;
import com.lms.payment.entity.Payment;
import com.lms.payment.entity.Subscription;
import com.lms.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller exposing all payment and subscription endpoints.
 *
 * Base path: /api/payments
 * All endpoints are JWT-protected (enforced by SecurityConfig + JwtAuthFilter).
 */
@RestController
@RequestMapping({"/api/payments", "/api/payment", "/payments"})
@RequiredArgsConstructor
@Tag(name = "Payment & Subscription", description = "Manage course payments and subscription plans")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentResource {

    private static final Logger log = LoggerFactory.getLogger(PaymentResource.class);

    private final PaymentService paymentService;

    // ── Payment Endpoints ────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Process a new payment for a course")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest req) {
        log.info("POST /api/payments - studentId={} courseTitle={}", req.getStudentId(), req.getCourseTitle());

        Payment payment = mapToPaymentEntity(req);
        
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        if (req.getCourseTitle() != null) {
            metadata.put("courseTitle", req.getCourseTitle());
        }
        
        Payment saved = paymentService.processPaymentWithMetadata(payment, metadata);

        return ResponseEntity.status(HttpStatus.CREATED).body(toPaymentResponse(saved, "Payment processed successfully"));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get all payments by a student")
    public ResponseEntity<List<PaymentResponse>> getByStudent(@PathVariable int studentId) {
        log.info("GET /api/payments/student/{}", studentId);
        List<PaymentResponse> list = paymentService.getPaymentsByStudent(studentId)
            .stream().map(p -> toPaymentResponse(p, null)).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get all payments for a course")
    public ResponseEntity<List<PaymentResponse>> getByCourse(@PathVariable int courseId) {
        log.info("GET /api/payments/course/{}", courseId);
        List<PaymentResponse> list = paymentService.getPaymentsByCourse(courseId)
            .stream().map(p -> toPaymentResponse(p, null)).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a successful payment")
    public ResponseEntity<String> refund(@PathVariable int paymentId) {
        log.info("PUT /api/payments/{}/refund", paymentId);
        paymentService.refundPayment(paymentId);
        return ResponseEntity.ok("Refund initiated for paymentId: " + paymentId);
    }

    @GetMapping("/razorpay/order")
    @Operation(summary = "Create a Razorpay order")
    public ResponseEntity<String> createRazorpayOrder(@RequestParam double amount) {
        log.info("GET /api/payments/razorpay/order - amount={}", amount);
        String orderId = paymentService.createRazorpayOrder(amount);
        return ResponseEntity.ok(orderId);
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Verify Razorpay signature")
    public ResponseEntity<Boolean> verifyRazorpay(@RequestBody Map<String, String> data) {
        log.info("POST /api/payments/razorpay/verify");
        boolean isValid = paymentService.verifyRazorpayPayment(
            data.get("razorpay_order_id"),
            data.get("razorpay_payment_id"),
            data.get("razorpay_signature")
        );
        return ResponseEntity.ok(isValid);
    }

    // ── Subscription Endpoints ───────────────────────────────────────────────

    @PostMapping("/subscriptions")
    @Operation(summary = "Create a subscription for a student")
    public ResponseEntity<SubscriptionResponse> subscribe(@Valid @RequestBody SubscriptionRequest req) {
        log.info("POST /api/payments/subscriptions - studentId={} plan={}", req.getStudentId(), req.getPlan());
        Subscription sub = paymentService.subscribe(req.getStudentId(), req.getPlan());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSubResponse(sub, "Subscription created"));
    }

    @DeleteMapping("/subscriptions/{studentId}")
    @Operation(summary = "Cancel the active subscription of a student")
    public ResponseEntity<String> cancelSubscription(@PathVariable int studentId) {
        log.info("DELETE /api/payments/subscriptions/{}", studentId);
        paymentService.cancelSubscription(studentId);
        return ResponseEntity.ok("Subscription cancelled for studentId: " + studentId);
    }

    @PutMapping("/subscriptions/{studentId}/renew")
    @Operation(summary = "Renew the active subscription of a student")
    public ResponseEntity<SubscriptionResponse> renewSubscription(@PathVariable int studentId) {
        log.info("PUT /api/payments/subscriptions/{}/renew", studentId);
        Subscription sub = paymentService.renewSubscription(studentId);
        return ResponseEntity.ok(toSubResponse(sub, "Subscription renewed"));
    }

    @GetMapping("/subscriptions/{studentId}")
    @Operation(summary = "Get the active subscription for a student")
    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable int studentId) {
        log.info("GET /api/payments/subscriptions/{}", studentId);
        Subscription sub = paymentService.getSubscriptionByStudent(studentId)
            .orElseThrow(() -> new com.lms.payment.exception.SubscriptionNotFoundException(
                "No active subscription for studentId: " + studentId));
        return ResponseEntity.ok(toSubResponse(sub, null));
    }

    @GetMapping("/subscriptions/{studentId}/active")
    @Operation(summary = "Check whether a student has an active subscription")
    public ResponseEntity<Boolean> isActive(@PathVariable int studentId) {
        log.info("GET /api/payments/subscriptions/{}/active", studentId);
        return ResponseEntity.ok(paymentService.isSubscriptionActive(studentId));
    }

    @GetMapping("/admin/stats")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get financial statistics for admin")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        log.info("GET /api/payments/admin/stats");
        return ResponseEntity.ok(paymentService.getAdminStats());
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private Payment mapToPaymentEntity(PaymentRequest req) {
        Payment p = new Payment();
        p.setStudentId(req.getStudentId());
        p.setCourseId(req.getCourseId());
        p.setAmount(req.getAmount());
        p.setMode(req.getMode());
        p.setCurrency(req.getCurrency());
        p.setTransactionId(req.getTransactionId());
        return p;
    }

    private PaymentResponse toPaymentResponse(Payment p, String message) {
        return PaymentResponse.builder()
            .paymentId(p.getPaymentId())
            .studentId(p.getStudentId())
            .courseId(p.getCourseId())
            .amount(p.getAmount())
            .status(p.getStatus())
            .mode(p.getMode())
            .transactionId(p.getTransactionId())
            .paidAt(p.getPaidAt())
            .currency(p.getCurrency())
            .message(message)
            .build();
    }

    private SubscriptionResponse toSubResponse(Subscription s, String message) {
        return SubscriptionResponse.builder()
            .subscriptionId(s.getSubscriptionId())
            .studentId(s.getStudentId())
            .plan(s.getPlan())
            .startDate(s.getStartDate())
            .endDate(s.getEndDate())
            .status(s.getStatus())
            .amountPaid(s.getAmountPaid())
            .autoRenew(s.isAutoRenew())
            .message(message)
            .build();
    }
}
