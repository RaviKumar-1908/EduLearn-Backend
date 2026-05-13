package com.lms.notification.resource;

import com.lms.notification.dto.EmailRequest;
import com.lms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification/email")
@RequiredArgsConstructor
public class EmailController {

    private final NotificationService notificationService;

    @PostMapping("/welcome")
    public ResponseEntity<String> sendManualWelcome(@RequestParam String email, @RequestParam String name) {
        notificationService.sendWelcomeEmail(email, name);
        return ResponseEntity.ok("Welcome email queued successfully");
    }

    @PostMapping("/otp")
    public ResponseEntity<String> sendManualOtp(@RequestParam String email, @RequestParam String otp) {
        notificationService.sendOtpEmail(email, otp);
        return ResponseEntity.ok("OTP email queued successfully");
    }

    @PostMapping("/custom")
    public ResponseEntity<String> sendCustomEmail(@RequestBody EmailRequest request) {
        notificationService.sendEmailAlert(request.getTo(), request.getSubject(), "Custom content");
        return ResponseEntity.ok("Custom email queued successfully");
    }

    @PostMapping("/certificate")
    public ResponseEntity<String> sendCertificateEmail(@RequestBody EmailRequest request) {
        notificationService.sendCertificateEmail(request);
        return ResponseEntity.ok("Certificate email sent successfully");
    }
}
