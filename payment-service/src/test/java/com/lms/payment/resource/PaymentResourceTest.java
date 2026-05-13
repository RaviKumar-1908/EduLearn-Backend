package com.lms.payment.resource;

import com.lms.payment.dto.*;
import com.lms.payment.entity.Payment;
import com.lms.payment.entity.Subscription;
import com.lms.payment.service.PaymentService;
import com.lms.payment.filter.JwtAuthFilter;
import com.lms.payment.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentResource.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    private Payment mockPayment;
    private Subscription mockSubscription;

    @BeforeEach
    void setUp() {
        mockPayment = new Payment();
        mockPayment.setPaymentId(1);
        mockPayment.setStudentId(10);
        mockPayment.setAmount(100.0);
        mockPayment.setStatus("SUCCESS");

        mockSubscription = new Subscription();
        mockSubscription.setSubscriptionId(100);
        mockSubscription.setStudentId(10);
        mockSubscription.setPlan("MONTHLY");
        mockSubscription.setStatus("ACTIVE");
    }

    @Test
    void processPayment_success() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setStudentId(10);
        request.setCourseId(101);
        request.setAmount(100.0);

        when(paymentService.processPayment(any())).thenReturn(mockPayment);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(1));
    }

    @Test
    void processPayment_error() throws Exception {
        when(paymentService.processPayment(any())).thenThrow(new RuntimeException("Payment failed"));

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getByStudent_success() throws Exception {
        when(paymentService.getPaymentsByStudent(10)).thenReturn(List.of(mockPayment));

        mockMvc.perform(get("/api/payments/student/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByCourse_success() throws Exception {
        when(paymentService.getPaymentsByCourse(101)).thenReturn(List.of(mockPayment));

        mockMvc.perform(get("/api/payments/course/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void refund_success() throws Exception {
        doNothing().when(paymentService).refundPayment(1);

        mockMvc.perform(put("/api/payments/1/refund"))
                .andExpect(status().isOk());
    }

    @Test
    void createRazorpayOrder_success() throws Exception {
        when(paymentService.createRazorpayOrder(100.0)).thenReturn("ORDER123");

        mockMvc.perform(get("/api/payments/razorpay/order").param("amount", "100.0"))
                .andExpect(status().isOk())
                .andExpect(content().string("ORDER123"));
    }

    @Test
    void verifyRazorpay_success() throws Exception {
        when(paymentService.verifyRazorpayPayment(anyString(), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/payments/razorpay/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"razorpay_order_id\":\"1\", \"razorpay_payment_id\":\"2\", \"razorpay_signature\":\"3\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void subscribe_success() throws Exception {
        SubscriptionRequest request = new SubscriptionRequest();
        request.setStudentId(10);
        request.setPlan("MONTHLY");

        when(paymentService.subscribe(eq(10), eq("MONTHLY"))).thenReturn(mockSubscription);

        mockMvc.perform(post("/api/payments/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionId").value(100));
    }

    @Test
    void cancelSubscription_success() throws Exception {
        doNothing().when(paymentService).cancelSubscription(10);

        mockMvc.perform(delete("/api/payments/subscriptions/10"))
                .andExpect(status().isOk());
    }

    @Test
    void renewSubscription_success() throws Exception {
        when(paymentService.renewSubscription(10)).thenReturn(mockSubscription);

        mockMvc.perform(put("/api/payments/subscriptions/10/renew"))
                .andExpect(status().isOk());
    }

    @Test
    void getSubscription_success() throws Exception {
        when(paymentService.getSubscriptionByStudent(10)).thenReturn(Optional.of(mockSubscription));

        mockMvc.perform(get("/api/payments/subscriptions/10"))
                .andExpect(status().isOk());
    }

    @Test
    void getSubscription_notFound() throws Exception {
        when(paymentService.getSubscriptionByStudent(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/subscriptions/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void isActive_success() throws Exception {
        when(paymentService.isSubscriptionActive(10)).thenReturn(true);

        mockMvc.perform(get("/api/payments/subscriptions/10/active"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getAdminStats_success() throws Exception {
        when(paymentService.getAdminStats()).thenReturn(Map.of("totalRevenue", 1000.0));

        mockMvc.perform(get("/api/payments/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(1000.0));
    }
}
