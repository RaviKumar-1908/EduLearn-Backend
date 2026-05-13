package com.lms.payment.service;

import com.lms.payment.entity.Payment;
import com.lms.payment.entity.Subscription;
import com.lms.payment.exception.InvalidPaymentException;
import com.lms.payment.exception.PaymentNotFoundException;
import com.lms.payment.exception.SubscriptionNotFoundException;
import com.lms.payment.messaging.PaymentEventPublisher;
import com.lms.payment.repository.PaymentRepository;
import com.lms.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment mockPayment;
    private Subscription mockSubscription;

    @BeforeEach
    void setUp() {
        // Inject @Value fields manually
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "test_key_id");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_key_secret");
        ReflectionTestUtils.setField(paymentService, "exchange", "lms.events.exchange");

        mockPayment = new Payment();
        mockPayment.setPaymentId(1);
        mockPayment.setStudentId(10);
        mockPayment.setCourseId(101);
        mockPayment.setAmount(499.0);
        mockPayment.setStatus("SUCCESS");
        mockPayment.setMode("UPI");
        mockPayment.setTransactionId("TXN-10-101-abc123");
        mockPayment.setCurrency("INR");
        mockPayment.setPaidAt(LocalDateTime.now());

        mockSubscription = new Subscription();
        mockSubscription.setSubscriptionId(1);
        mockSubscription.setStudentId(10);
        mockSubscription.setPlan("MONTHLY");
        mockSubscription.setStatus("ACTIVE");
        mockSubscription.setStartDate(LocalDate.now());
        mockSubscription.setEndDate(LocalDate.now().plusMonths(1));
        mockSubscription.setAmountPaid(499.0);
        mockSubscription.setAutoRenew(false);
    }

    // ==================== PROCESS PAYMENT ====================

    @Test
    void processPayment_upi_success() {
        Payment payment = new Payment();
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(499.0);
        payment.setMode("UPI");
        payment.setTransactionId("TXN123");
        payment.setCurrency("INR");

        when(paymentRepository.findByTransactionId("TXN123"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        doNothing().when(eventPublisher)
                .publishPaymentSuccess(any(Payment.class), any());

        Payment result = paymentService.processPayment(payment);

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPayment_freePayment_success() {
        Payment payment = new Payment();
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(0.0);
        payment.setCurrency("INR");

        when(paymentRepository.findByTransactionId(anyString()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        doNothing().when(eventPublisher)
                .publishPaymentSuccess(any(Payment.class), any());

        Payment result = paymentService.processPayment(payment);

        assertNotNull(result);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPayment_negativeAmount_throwsException() {
        Payment payment = new Payment();
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(-100.0);
        payment.setMode("UPI");

        assertThrows(InvalidPaymentException.class,
                () -> paymentService.processPayment(payment));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_duplicateTransactionId_throwsException() {
        Payment payment = new Payment();
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(499.0);
        payment.setMode("UPI");
        payment.setTransactionId("TXN123");
        payment.setCurrency("INR");

        when(paymentRepository.findByTransactionId("TXN123"))
                .thenReturn(Optional.of(mockPayment));

        assertThrows(InvalidPaymentException.class,
                () -> paymentService.processPayment(payment));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_invalidMode_throwsException() {
        Payment payment = new Payment();
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(499.0);
        payment.setMode("CASH"); // invalid mode

        assertThrows(InvalidPaymentException.class,
                () -> paymentService.processPayment(payment));
    }

    @Test
    void processPayment_missingMode_throwsException() {
        Payment payment = new Payment();
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(499.0);
        payment.setMode(null);

        assertThrows(InvalidPaymentException.class,
                () -> paymentService.processPayment(payment));
    }

    @Test
    void processPayment_invalidCurrency_throwsException() {
        Payment payment = new Payment();
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(499.0);
        payment.setMode("UPI");
        payment.setCurrency("INVALID");

        assertThrows(InvalidPaymentException.class,
                () -> paymentService.processPayment(payment));
    }

    @Test
    void processPayment_invalidTransactionId_throwsException() {
        Payment payment = new Payment();
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(499.0);
        payment.setMode("UPI");
        payment.setCurrency("INR");
        payment.setTransactionId("ab"); // too short - less than 3 chars

        assertThrows(InvalidPaymentException.class,
                () -> paymentService.processPayment(payment));
    }

    // ==================== PROCESS PAYMENT WITH METADATA ====================

    @Test
    void processPaymentWithMetadata_success() {
        Payment payment = new Payment();
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(499.0);
        payment.setMode("CARD");
        payment.setTransactionId("TXN-META-123");
        payment.setCurrency("INR");

        Map<String, Object> metadata = Map.of("courseName", "Java Basics");

        when(paymentRepository.findByTransactionId("TXN-META-123"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        doNothing().when(eventPublisher)
                .publishPaymentSuccess(any(Payment.class), any());

        Payment result = paymentService.processPaymentWithMetadata(payment, metadata);

        assertNotNull(result);
        verify(eventPublisher, times(1))
                .publishPaymentSuccess(any(Payment.class), eq(metadata));
    }

    // ==================== GET PAYMENTS ====================

    @Test
    void getPaymentsByStudent_success() {
        when(paymentRepository.findByStudentId(10)).thenReturn(List.of(mockPayment));

        List<Payment> result = paymentService.getPaymentsByStudent(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getStudentId());
        verify(paymentRepository, times(1)).findByStudentId(10);
    }

    @Test
    void getPaymentsByStudent_empty() {
        when(paymentRepository.findByStudentId(99)).thenReturn(List.of());

        List<Payment> result = paymentService.getPaymentsByStudent(99);

        assertTrue(result.isEmpty());
    }

    @Test
    void getPaymentsByCourse_success() {
        when(paymentRepository.findByCourseId(101)).thenReturn(List.of(mockPayment));

        List<Payment> result = paymentService.getPaymentsByCourse(101);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentRepository, times(1)).findByCourseId(101);
    }

    @Test
    void getPaymentsByCourse_empty() {
        when(paymentRepository.findByCourseId(999)).thenReturn(List.of());

        List<Payment> result = paymentService.getPaymentsByCourse(999);

        assertTrue(result.isEmpty());
    }

    // ==================== REFUND PAYMENT ====================

    @Test
    void refundPayment_success() {
        when(paymentRepository.findById(1)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        doNothing().when(eventPublisher).publishRefund(any(Payment.class));

        paymentService.refundPayment(1);

        assertEquals("REFUNDED", mockPayment.getStatus());
        verify(paymentRepository, times(1)).save(mockPayment);
        verify(eventPublisher, times(1)).publishRefund(mockPayment);
    }

    @Test
    void refundPayment_notFound_throwsException() {
        when(paymentRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.refundPayment(99));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundPayment_notSuccessStatus_throwsException() {
        mockPayment.setStatus("REFUNDED");
        when(paymentRepository.findById(1)).thenReturn(Optional.of(mockPayment));

        assertThrows(InvalidPaymentException.class,
                () -> paymentService.refundPayment(1));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundPayment_pendingStatus_throwsException() {
        mockPayment.setStatus("PENDING");
        when(paymentRepository.findById(1)).thenReturn(Optional.of(mockPayment));

        assertThrows(InvalidPaymentException.class,
                () -> paymentService.refundPayment(1));
    }

    // ==================== SUBSCRIBE ====================

    @Test
    void subscribe_monthly_success() {
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(mockSubscription);
        doNothing().when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        Subscription result = paymentService.subscribe(10, "MONTHLY");

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("MONTHLY", result.getPlan());
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    void subscribe_annual_success() {
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.empty());

        Subscription annualSub = new Subscription();
        annualSub.setSubscriptionId(2);
        annualSub.setStudentId(10);
        annualSub.setPlan("ANNUAL");
        annualSub.setStatus("ACTIVE");
        annualSub.setStartDate(LocalDate.now());
        annualSub.setEndDate(LocalDate.now().plusYears(1));
        annualSub.setAmountPaid(3999.0);

        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(annualSub);
        doNothing().when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        Subscription result = paymentService.subscribe(10, "ANNUAL");

        assertNotNull(result);
        assertEquals(3999.0, result.getAmountPaid());
    }

    @Test
    void subscribe_cancelsPreviousActive_thenCreatesNew() {
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.of(mockSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(mockSubscription);
        doNothing().when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        paymentService.subscribe(10, "MONTHLY");

        // save called twice: once to cancel old, once to save new
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
    }

    // ==================== CANCEL SUBSCRIPTION ====================

    @Test
    void cancelSubscription_success() {
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.of(mockSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(mockSubscription);

        paymentService.cancelSubscription(10);

        assertEquals("CANCELLED", mockSubscription.getStatus());
        assertFalse(mockSubscription.isAutoRenew());
        verify(subscriptionRepository, times(1)).save(mockSubscription);
    }

    @Test
    void cancelSubscription_noActiveSubscription_throwsException() {
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class,
                () -> paymentService.cancelSubscription(10));
        verify(subscriptionRepository, never()).save(any());
    }

    // ==================== RENEW SUBSCRIPTION ====================

    @Test
    void renewSubscription_success() {
        mockSubscription.setEndDate(LocalDate.now().plusDays(5));
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.of(mockSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(mockSubscription);

        Subscription result = paymentService.renewSubscription(10);

        assertNotNull(result);
        verify(subscriptionRepository, times(1)).save(mockSubscription);
    }

    @Test
    void renewSubscription_expiredEndDate_extendsFromToday() {
        mockSubscription.setEndDate(LocalDate.now().minusDays(5)); // already expired
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.of(mockSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(mockSubscription);

        Subscription result = paymentService.renewSubscription(10);

        assertNotNull(result);
        verify(subscriptionRepository, times(1)).save(mockSubscription);
    }

    @Test
    void renewSubscription_noActiveSubscription_throwsException() {
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class,
                () -> paymentService.renewSubscription(10));
    }

    // ==================== GET SUBSCRIPTION ====================

    @Test
    void getSubscriptionByStudent_success() {
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.of(mockSubscription));

        Optional<Subscription> result = paymentService.getSubscriptionByStudent(10);

        assertTrue(result.isPresent());
        assertEquals("MONTHLY", result.get().getPlan());
    }

    @Test
    void getSubscriptionByStudent_notFound_returnsEmpty() {
        when(subscriptionRepository.findByStudentIdAndStatus(99, "ACTIVE"))
                .thenReturn(Optional.empty());

        Optional<Subscription> result = paymentService.getSubscriptionByStudent(99);

        assertFalse(result.isPresent());
    }

    // ==================== IS SUBSCRIPTION ACTIVE ====================

    @Test
    void isSubscriptionActive_activeAndNotExpired_returnsTrue() {
        mockSubscription.setEndDate(LocalDate.now().plusDays(10));
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.of(mockSubscription));

        boolean result = paymentService.isSubscriptionActive(10);

        assertTrue(result);
    }

    @Test
    void isSubscriptionActive_noSubscription_returnsFalse() {
        when(subscriptionRepository.findByStudentIdAndStatus(99, "ACTIVE"))
                .thenReturn(Optional.empty());

        boolean result = paymentService.isSubscriptionActive(99);

        assertFalse(result);
    }

    @Test
    void isSubscriptionActive_expiredEndDate_returnsFalse() {
        mockSubscription.setEndDate(LocalDate.now().minusDays(1));
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE"))
                .thenReturn(Optional.of(mockSubscription));

        boolean result = paymentService.isSubscriptionActive(10);

        assertFalse(result);
    }

    // ==================== ADMIN STATS ====================

    @Test
    void getAdminStats_success() {
        Payment successPayment = new Payment();
        successPayment.setStatus("SUCCESS");
        successPayment.setAmount(499.0);
        successPayment.setPaidAt(LocalDateTime.now());

        Payment refundedPayment = new Payment();
        refundedPayment.setStatus("REFUNDED");
        refundedPayment.setAmount(199.0);
        refundedPayment.setPaidAt(LocalDateTime.now());

        when(paymentRepository.findAll())
                .thenReturn(List.of(successPayment, refundedPayment));
        when(subscriptionRepository.count()).thenReturn(5L);

        Map<String, Object> stats = paymentService.getAdminStats();

        assertNotNull(stats);
        assertEquals(499.0, stats.get("totalRevenue"));
        assertEquals(1L, stats.get("totalTransactions"));
        assertEquals(5L, stats.get("totalSubscriptions"));
        assertEquals("INR", stats.get("currency"));
        assertTrue(stats.containsKey("recentPayments"));
    }

    @Test
    void getAdminStats_noPayments_zeroRevenue() {
        when(paymentRepository.findAll()).thenReturn(List.of());
        when(subscriptionRepository.count()).thenReturn(0L);

        Map<String, Object> stats = paymentService.getAdminStats();

        assertEquals(0.0, stats.get("totalRevenue"));
        assertEquals(0L, stats.get("totalTransactions"));
    }

    @Test
    void processPayment_eventPublisherFails_stillSavesPayment() {
        when(paymentRepository.findByTransactionId(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(mockPayment);
        doThrow(new RuntimeException("MQ Down")).when(eventPublisher).publishPaymentSuccess(any(), any());

        Payment result = paymentService.processPayment(mockPayment);
        assertNotNull(result);
        verify(paymentRepository).save(any()); // Should still save despite MQ error
    }

    @Test
    void refundPayment_eventPublisherFails_stillUpdatesStatus() {
        when(paymentRepository.findById(1)).thenReturn(Optional.of(mockPayment));
        doThrow(new RuntimeException("MQ Down")).when(eventPublisher).publishRefund(any());

        paymentService.refundPayment(1);
        assertEquals("REFUNDED", mockPayment.getStatus());
        verify(paymentRepository).save(mockPayment);
    }

    @Test
    void subscribe_freeTrial_success() {
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenReturn(mockSubscription);

        Subscription result = paymentService.subscribe(10, "FREE");
        assertNotNull(result);
        // Helper logic check via internal interaction or if we returned the saved
        // object
        verify(subscriptionRepository).save(any());
    }

    @Test
    void subscribe_emailFails_stillSubscribes() {
        when(subscriptionRepository.findByStudentIdAndStatus(10, "ACTIVE")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenReturn(mockSubscription);
        doThrow(new RuntimeException("Mail Server Down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        Subscription result = paymentService.subscribe(10, "MONTHLY");
        assertNotNull(result);
        verify(subscriptionRepository).save(any());
    }

    @Test
    void verifyRazorpayPayment_validSignature_returnsTrue() {
        try (org.mockito.MockedStatic<com.razorpay.Utils> utils = mockStatic(com.razorpay.Utils.class)) {
            utils.when(() -> com.razorpay.Utils.verifySignature(anyString(), anyString(), anyString()))
                    .thenReturn(true);

            boolean result = paymentService.verifyRazorpayPayment("order_123", "pay_123", "sig_123");
            assertTrue(result);
        }
    }

    @Test
    void verifyRazorpayPayment_invalidSignature_returnsFalse() {
        try (org.mockito.MockedStatic<com.razorpay.Utils> utils = mockStatic(com.razorpay.Utils.class)) {
            utils.when(() -> com.razorpay.Utils.verifySignature(anyString(), anyString(), anyString()))
                    .thenReturn(false);

            boolean result = paymentService.verifyRazorpayPayment("order_123", "pay_123", "sig_123");
            assertFalse(result);
        }
    }

    @Test
    void verifyRazorpayPayment_exception_returnsFalse() {
        try (org.mockito.MockedStatic<com.razorpay.Utils> utils = mockStatic(com.razorpay.Utils.class)) {
            utils.when(() -> com.razorpay.Utils.verifySignature(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Verify Error"));

            boolean result = paymentService.verifyRazorpayPayment("order_123", "pay_123", "sig_123");
            assertFalse(result);
        }
    }

    @Test
    void createRazorpayOrder_exception_throwsRuntimeException() {
        // Since we can't easily mock 'new RazorpayClient' without extra dependencies,
        // this will naturally throw an exception if credentials are fake/invalid or
        // network is down.
        // But for unit test, we just want to see it catch and rethrow.
        assertThrows(RuntimeException.class, () -> paymentService.createRazorpayOrder(100.0));
    }
}