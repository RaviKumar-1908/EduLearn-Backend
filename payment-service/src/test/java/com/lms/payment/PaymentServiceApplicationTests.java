package com.lms.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "JWT_SECRET=testSecretKeyLongEnoughForHmacSha256Algorithm",
    "JWT_EXPIRATION=3600000",
    "RAZORPAY_KEY_ID=test_id",
    "RAZORPAY_KEY_SECRET=test_secret"
})
class PaymentServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
