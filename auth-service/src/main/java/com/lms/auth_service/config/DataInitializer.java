package com.lms.auth_service.config;

import com.lms.auth_service.entity.User;
import com.lms.auth_service.enums.ApprovalStatus;
import com.lms.auth_service.enums.AuthProvider;
import com.lms.auth_service.enums.Role;
import com.lms.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Seed Admin User
        if (!userRepository.existsByEmail("admin123@gmail.com")) {
            User admin = User.builder()
                    .fullName("System Admin")
                    .email("admin123@gmail.com")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .provider(AuthProvider.LOCAL)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            System.out.println("Admin user seeded successfully!");
        }
    }
}
