package com.lms.auth_service.service.impl;

import com.lms.auth_service.dto.RegisterRequestDto;
import com.lms.auth_service.dto.AuthResponseDto;
import com.lms.auth_service.entity.User;
import com.lms.auth_service.enums.ApprovalStatus;
import com.lms.auth_service.enums.AuthProvider;
import com.lms.auth_service.enums.Role;
import com.lms.auth_service.repository.UserRepository;
import com.lms.auth_service.service.AuthService;
import com.lms.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String EXCHANGE = "lms.events.exchange";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RabbitTemplate rabbitTemplate;

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Central publish method — every event goes through here.
     * Logs success/failure consistently; never throws.
     */
    private void publish(String routingKey, Map<String, Object> event) {
        log.info("[RabbitMQ] ▶ Publishing event | exchange={} | routingKey={} | userId={}",
                EXCHANGE, routingKey, event.get("userId"));
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("[RabbitMQ] ✔ Event published | routingKey={}", routingKey);
        } catch (AmqpException e) {
            log.error("[RabbitMQ] ✘ FAILED to publish | routingKey={} | error={}",
                    routingKey, e.getMessage(), e);
        } catch (Exception e) {
            log.error("[RabbitMQ] ✘ Unexpected error publishing | routingKey={} | error={}",
                    routingKey, e.getMessage(), e);
        }
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Override
    public User register(RegisterRequestDto request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with this email already exists!");
        }

        Role role = Role.valueOf(request.getRole().toUpperCase());

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .provider(AuthProvider.LOCAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .gender(request.getGender())
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());

        Map<String, Object> event = new HashMap<>();
        event.put("userId", savedUser.getUserId());
        event.put("email", savedUser.getEmail());
        event.put("fullName", savedUser.getFullName());
        event.put("title", "Welcome to our Platform!");
        event.put("message", "Welcome " + savedUser.getFullName() + "! We are glad to have you on board.");
        event.put("type", "AUTH_REGISTER");
        publish("notification.auth.register", event);

        // If Instructor, notify Admin
        if (role == Role.INSTRUCTOR) {
            Map<String, Object> adminEvent = new HashMap<>();
            adminEvent.put("userId", 1); 
            adminEvent.put("title", "New Instructor Application! 🎓");
            adminEvent.put("message", "A new instructor (" + savedUser.getFullName() + ") has registered and requires approval.");
            adminEvent.put("type", "ADMIN_ALERT");
            publish("notification.auth.admin.alert", adminEvent);
        }

        return savedUser;
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Override
    public AuthResponseDto login(String email, String password) {
        log.info("Attempting login for user: {}", email);

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else if ("admin123@gmail.com".equals(email)) {
            log.info("Admin user not found. Creating default admin account.");
            user = User.builder()
                    .email("admin123@gmail.com")
                    .fullName("LMS Administrator")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .provider(AuthProvider.LOCAL)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);
        } else {
            throw new RuntimeException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Your account has been suspended. Please contact support.");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            log.warn("Login failed: User {} has no password set (Google-only account)", email);
            throw new RuntimeException(
                    "This account is linked with Google. Please use 'Continue with Google' to sign in.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: Incorrect password for user {}", email);
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(
                user.getUserId(), user.getEmail(), user.getRole().name(),
                user.getFullName(), user.getProfilePicUrl(), user.getGender());

        log.info("Successfully generated JWT for user: {}", email);

        Map<String, Object> event = new HashMap<>();
        event.put("userId", user.getUserId());
        event.put("email", user.getEmail());
        event.put("type", "AUTH_LOGIN");
        
        if (user.getRole() == Role.INSTRUCTOR) {
            event.put("title", "Dashboard Ready! 🎯");
            event.put("message", "Welcome back " + user.getFullName() + ". Your instructor dashboard is synchronized and ready for management.");
        } else if (user.getRole() == Role.ADMIN) {
            event.put("title", "Admin Access Logged 🛡️");
            event.put("message", "Administrative session started for " + user.getFullName());
        } else {
            event.put("title", "Welcome Back! 👋");
            event.put("message", "Happy learning, " + user.getFullName() + "! You're now logged in.");
        }
        
        publish("notification.auth.login", event);

        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .approvalStatus(user.getApprovalStatus() != null
                        ? user.getApprovalStatus().name()
                        : "APPROVED")
                .build();
    }

    // ── forgotPassword ────────────────────────────────────────────────────────

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));
        log.info("Processing forgot password for {}", email);

        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        String otp = String.valueOf(100000 + secureRandom.nextInt(900000));
        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        Map<String, Object> event = new HashMap<>();
        event.put("userId", user.getUserId());
        event.put("email", user.getEmail());
        event.put("fullName", user.getFullName());
        event.put("title", "EduLearn: Password Reset OTP");
        event.put("message", "Hello " + user.getFullName()
                + ", your password reset code is: " + otp + ". This code expires in 10 minutes.");
        event.put("otp", otp);
        event.put("type", "AUTH_FORGOT_PASSWORD");
        publish("notification.auth.forgot_password", event);

        log.info("Processed forgot password for user: {}", email);
    }

    // ── updateUserStatus ──────────────────────────────────────────────────────

    @Override
    public void updateUserStatus(int userId, boolean active) {
        User user = userRepository.findByUserId(userId);
        if (user == null)
            return;

        user.setActive(active);
        userRepository.save(user);

        Map<String, Object> event = new HashMap<>();
        event.put("userId", user.getUserId());
        event.put("email", user.getEmail());
        event.put("fullName", user.getFullName());
        event.put("type", "AUTH_STATUS_CHANGE");

        if (active) {
            event.put("title", "Account Access Restored");
            event.put("message", "Hello " + user.getFullName()
                    + ", your account access has been granted again. Welcome back!");
        } else {
            event.put("title", "Account Restricted");
            event.put("message", "Hello " + user.getFullName()
                    + ", your account has been restricted by the administrator. "
                    + "Please contact our support team if you believe this is an error.");
        }

        publish("notification.auth.status", event);
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));
        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp))
            throw new RuntimeException("Invalid OTP");
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP has expired. Please request a new one.");

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        log.info("Password successfully reset for user: {}", email);
    }

    // ── profile & search ──────────────────────────────────────────────────────

    @Override
    public User updateProfile(int userId, com.lms.auth_service.dto.ProfileUpdateRequestDto userDetails) {
        log.info("Updating profile for userId: {}", userId);
        User existingUser = userRepository.findByUserId(userId);
        if (existingUser == null)
            throw new RuntimeException("User not found");

        existingUser.setFullName(userDetails.getFullName());
        existingUser.setMobile(userDetails.getMobile());
        existingUser.setBio(userDetails.getBio());
        existingUser.setProfilePicUrl(userDetails.getProfilePicUrl());
        if (userDetails.getGender() != null)
            existingUser.setGender(userDetails.getGender());

        User updated = userRepository.save(existingUser);
        log.info("Profile updated for userId: {}", userId);
        return updated;
    }

    @Override
    public List<User> searchUsers(String query) {
        return userRepository.findByFullNameContaining(query);
    }

    // ── user retrieval ────────────────────────────────────────────────────────

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public User getUserById(int userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null)
            throw new RuntimeException("User not found");
        user.setPasswordHash(null);
        return user;
    }

    @Override
    public List<User> getUsersByIds(List<Integer> userIds) {
        return userRepository.findByUserIdIn(userIds).stream()
                .peek(u -> u.setPasswordHash(null))
                .toList();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ── account management ──────────────────────────────────────────────────

    @Override
    public void changePassword(int userId, String newPassword) {
        User user = userRepository.findByUserId(userId);
        if (user == null)
            throw new RuntimeException("User not found");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void deleteUser(int userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public void updateUserRole(int userId, String role) {
        User user = userRepository.findByUserId(userId);
        if (user != null) {
            user.setRole(Role.valueOf(role.toUpperCase()));
            userRepository.save(user);
        }
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));
        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp))
            return false;
        return user.getOtpExpiry() != null && user.getOtpExpiry().isAfter(LocalDateTime.now());
    }

    @Override
    public void logout(String token) {
    }

    @Override
    public boolean validateToken(String token) {
        return true;
    }

    @Override
    public String refreshToken(String token) {
        return "new-mock-jwt-token";
    }

    @Override
    public List<Integer> getAllAdminIds() {
        return userRepository.findAllByRole(Role.ADMIN).stream()
                .map(User::getUserId)
                .toList();
    }
}