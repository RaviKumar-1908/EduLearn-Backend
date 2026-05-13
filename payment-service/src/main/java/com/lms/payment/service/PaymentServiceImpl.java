package com.lms.payment.service;

import com.lms.payment.entity.Payment;
import com.lms.payment.entity.Subscription;
import com.lms.payment.exception.PaymentNotFoundException;
import com.lms.payment.exception.SubscriptionNotFoundException;
import com.lms.payment.exception.InvalidPaymentException;
import com.lms.payment.messaging.PaymentEventPublisher;
import com.lms.payment.repository.PaymentRepository;
import com.lms.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
// import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

/**
 * Core business logic for payments and subscriptions.
 *
 * Design decisions worth noting for an interview:
 * - @Transactional on write methods ensures DB consistency if anything throws
 * mid-way.
 * - Redis @Cacheable on read-heavy methods reduces DB round-trips.
 * - RabbitMQ events keep this service decoupled from Notification-Service.
 */
@Service
@RequiredArgsConstructor // Lombok generates constructor-injection for all final fields
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentEventPublisher eventPublisher;
    private final RabbitTemplate rabbitTemplate;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    // ────────────────────────────────────────────────────────────────────────
    // PAYMENT OPERATIONS
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Payment processPayment(Payment payment) {
        return processPaymentWithMetadata(payment, null);
    }

    @Override
    @Transactional
    public Payment processPaymentWithMetadata(Payment payment, java.util.Map<String, Object> metadata) {
        log.info("Processing payment with metadata for studentId={} courseId={}",
                payment.getStudentId(), payment.getCourseId());

        normalizePayment(payment);

        // Guard: duplicate transaction ID
        paymentRepository.findByTransactionId(payment.getTransactionId())
                .ifPresent(p -> {
                    throw new InvalidPaymentException(
                            "Duplicate transactionId: " + payment.getTransactionId());
                });

        payment.setPaidAt(LocalDateTime.now());
        payment.setStatus("SUCCESS");

        Payment saved = paymentRepository.save(payment);
        log.info("Payment persisted with paymentId={}", saved.getPaymentId());

        // Publish event to RabbitMQ so Notification-Service can send email/SMS
        try {
            eventPublisher.publishPaymentSuccess(saved, metadata);
        } catch (Exception e) {
            log.warn("Failed to publish payment success event for paymentId={}: {}", saved.getPaymentId(),
                    e.getMessage());
        }

        return saved;
    }

    private void normalizePayment(Payment payment) {
        if (payment.getAmount() < 0) {
            throw new InvalidPaymentException("Payment amount cannot be negative");
        }

        if (payment.getAmount() == 0.0) {
            payment.setMode("FREE");
            payment.setCurrency(
                    hasText(payment.getCurrency()) ? payment.getCurrency().toUpperCase(Locale.ROOT) : "INR");
            if (!hasText(payment.getTransactionId())) {
                payment.setTransactionId(
                        "FREE-" + payment.getStudentId() + "-" + payment.getCourseId() + "-" + UUID.randomUUID());
            }
            payment.setStatus("SUCCESS");
            return;
        }

        if (!hasText(payment.getMode())) {
            throw new InvalidPaymentException("Payment mode is required for paid courses");
        }
        String mode = payment.getMode().toUpperCase(Locale.ROOT);
        if (!List.of("CARD", "UPI", "NET_BANKING", "WALLET", "ONLINE").contains(mode)) {
            throw new InvalidPaymentException("Mode must be one of: CARD, UPI, NET_BANKING, WALLET, ONLINE");
        }
        payment.setMode(mode);

        if (!hasText(payment.getCurrency())) {
            payment.setCurrency("INR");
        } else {
            String currency = payment.getCurrency().toUpperCase(Locale.ROOT);
            if (!currency.matches("^[A-Z]{3}$")) {
                throw new InvalidPaymentException("Currency must be a 3-letter ISO code");
            }
            payment.setCurrency(currency);
        }

        if (!hasText(payment.getTransactionId())) {
            payment.setTransactionId(
                    "LOCAL-" + payment.getStudentId() + "-" + payment.getCourseId() + "-" + UUID.randomUUID());
        } else if (!payment.getTransactionId().matches("^[A-Za-z0-9_-]{3,100}$")) {
            throw new InvalidPaymentException("Transaction ID must be 3-100 alphanumeric characters");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public List<Payment> getPaymentsByStudent(int studentId) {
        log.debug("Fetching payments for studentId={}", studentId);
        return paymentRepository.findByStudentId(studentId);
    }

    @Override
    public List<Payment> getPaymentsByCourse(int courseId) {
        log.debug("Fetching payments for courseId={}", courseId);
        return paymentRepository.findByCourseId(courseId);
    }

    @Override
    @Transactional
    public void refundPayment(int paymentId) {
        log.info("Initiating refund for paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found with id: " + paymentId));

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new InvalidPaymentException(
                    "Only SUCCESS payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);
        try {
            eventPublisher.publishRefund(payment);
        } catch (Exception e) {
            log.warn("Failed to publish refund event for paymentId={}: {}", paymentId, e.getMessage());
        }
        log.info("Refund processed for paymentId={}", paymentId);
    }

    // ────────────────────────────────────────────────────────────────────────
    // SUBSCRIPTION OPERATIONS
    // ────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Subscription subscribe(int studentId, String plan) {
        log.info("Subscribing studentId={} to plan={}", studentId, plan);

        // Cancel any existing active subscription before creating a new one
        subscriptionRepository.findByStudentIdAndStatus(studentId, "ACTIVE")
                .ifPresent(existing -> {
                    existing.setStatus("CANCELLED");
                    subscriptionRepository.save(existing);
                    log.info("Previous ACTIVE subscription cancelled for studentId={}", studentId);
                });

        Subscription sub = new Subscription();
        sub.setStudentId(studentId);
        sub.setPlan(plan.toUpperCase());
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(calculateEndDate(plan));
        sub.setStatus("ACTIVE");
        sub.setAmountPaid(getPlanPrice(plan));
        sub.setAutoRenew(false);

        Subscription saved = subscriptionRepository.save(sub);
        log.info("Subscription created with subscriptionId={}", saved.getSubscriptionId());

        sendSubscriptionEmail(studentId, plan, saved.getEndDate());
        return saved;
    }

    @Override
    @Transactional
    public void cancelSubscription(int studentId) {
        log.info("Cancelling subscription for studentId={}", studentId);

        Subscription sub = subscriptionRepository
                .findByStudentIdAndStatus(studentId, "ACTIVE")
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "No active subscription for studentId: " + studentId));

        sub.setStatus("CANCELLED");
        sub.setAutoRenew(false);
        subscriptionRepository.save(sub);
        log.info("Subscription cancelled for studentId={}", studentId);
    }

    @Override
    @Transactional
    public Subscription renewSubscription(int studentId) {
        log.info("Renewing subscription for studentId={}", studentId);

        Subscription sub = subscriptionRepository
                .findByStudentIdAndStatus(studentId, "ACTIVE")
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "No active subscription to renew for studentId: " + studentId));

        // Extend from today or existing end-date, whichever is later
        LocalDate base = sub.getEndDate().isAfter(LocalDate.now())
                ? sub.getEndDate()
                : LocalDate.now();
        sub.setEndDate(extendDate(base, sub.getPlan()));
        subscriptionRepository.save(sub);

        log.info("Subscription renewed until {} for studentId={}", sub.getEndDate(), studentId);
        return sub;
    }

    @Override
    @Cacheable(value = "subscriptions", key = "#studentId")
    public Optional<Subscription> getSubscriptionByStudent(int studentId) {
        log.debug("Fetching subscription for studentId={}", studentId);
        return subscriptionRepository.findByStudentIdAndStatus(studentId, "ACTIVE");
    }

    @Override
    public boolean isSubscriptionActive(int studentId) {
        return subscriptionRepository
                .findByStudentIdAndStatus(studentId, "ACTIVE")
                .map(sub -> sub.getEndDate().isAfter(LocalDate.now()))
                .orElse(false);
    }

    // ────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ────────────────────────────────────────────────────────────────────────

    /** Computes the subscription end-date based on plan tier. */
    private LocalDate calculateEndDate(String plan) {
        return switch (plan.toUpperCase()) {
            case "MONTHLY" -> LocalDate.now().plusMonths(1);
            case "ANNUAL" -> LocalDate.now().plusYears(1);
            default -> LocalDate.now().plusDays(30); // FREE trial
        };
    }

    /** Extends an existing end-date by the plan's renewal period. */
    private LocalDate extendDate(LocalDate from, String plan) {
        return switch (plan.toUpperCase()) {
            case "MONTHLY" -> from.plusMonths(1);
            case "ANNUAL" -> from.plusYears(1);
            default -> from.plusDays(30);
        };
    }

    /** Returns the price (INR) for a plan tier. */
    private double getPlanPrice(String plan) {
        return switch (plan.toUpperCase()) {
            case "MONTHLY" -> 499.00;
            case "ANNUAL" -> 3999.00;
            default -> 0.0;
        };
    }

    /** Sends a subscription-confirmation notification via RabbitMQ. */
    private void sendSubscriptionEmail(int studentId, String plan, LocalDate endDate) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("userId", studentId);
            event.put("title", "Subscription Activated");
            event.put("message", "Your " + plan + " subscription is active until " + endDate + ".");
            event.put("type", "PAYMENT_SUBSCRIPTION");
            // eventPublisher could be extended or use generic template
            rabbitTemplate.convertAndSend(exchange, "notification.payment.subscription", event);
            log.info("Subscription confirmation event queued for studentId={}", studentId);
        } catch (Exception ex) {
            log.warn("Could not queue subscription event for studentId={}: {}", studentId, ex.getMessage());
        }
    }

    @Override
    public String createRazorpayOrder(double amount) {
        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (amount * 100)); // amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + UUID.randomUUID().toString().substring(0, 8));

            Order order = razorpay.orders.create(orderRequest);
            return order.get("id");
        } catch (Exception e) {
            log.error("Error creating Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Could not create Razorpay order");
        }
    }

    @Override
    public boolean verifyRazorpayPayment(String orderId, String paymentId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            return Utils.verifySignature(data, signature, razorpayKeySecret);
        } catch (Exception e) {
            log.error("Error verifying Razorpay signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public java.util.Map<String, Object> getAdminStats() {
        log.info("Fetching admin financial statistics");
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        List<Payment> allPayments = paymentRepository.findAll();
        double totalRevenue = allPayments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();

        long totalTransactions = allPayments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .count();

        long totalSubscriptions = subscriptionRepository.count();

        stats.put("totalRevenue", totalRevenue);
        stats.put("totalTransactions", totalTransactions);
        stats.put("totalSubscriptions", totalSubscriptions);
        stats.put("currency", "INR");

        // Add recent transactions for the analytics dashboard
        stats.put("recentPayments", allPayments.stream()
                .sorted((p1, p2) -> p2.getPaidAt().compareTo(p1.getPaidAt()))
                .limit(10)
                .collect(Collectors.toList()));

        return stats;
    }
}
