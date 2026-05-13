package com.lms.payment.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PaymentEntityTest {

    @Test
    void testPaymentGettersSetters() {
        Payment payment = new Payment();
        payment.setPaymentId(1);
        payment.setStudentId(10);
        payment.setCourseId(101);
        payment.setAmount(100.0);
        payment.setStatus("SUCCESS");
        payment.setMode("UPI");
        payment.setTransactionId("TXN123");
        payment.setCurrency("INR");
        payment.setPaidAt(LocalDateTime.now());

        assertEquals(1, payment.getPaymentId());
        assertEquals(100.0, payment.getAmount());
        assertEquals("SUCCESS", payment.getStatus());
    }

    @Test
    void testSubscriptionGettersSetters() {
        Subscription sub = new Subscription();
        sub.setSubscriptionId(1);
        sub.setStudentId(10);
        sub.setPlan("MONTHLY");
        sub.setStatus("ACTIVE");
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusMonths(1));
        sub.setAmountPaid(499.0);
        sub.setAutoRenew(true);

        assertEquals(1, sub.getSubscriptionId());
        assertEquals("MONTHLY", sub.getPlan());
        assertTrue(sub.isAutoRenew());
    }
}
