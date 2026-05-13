package com.lms.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "bug_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BugReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer userId;
    private String username;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String adminRemarks;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL
    private String category = "GENERAL"; // UI, FUNCTIONAL, PAYMENT, PERFORMANCE, SECURITY, GENERAL
    private String reporterRole; // STUDENT, INSTRUCTOR

    @Builder.Default
    private String status = "OPEN"; // OPEN, IN_PROGRESS, RESOLVED, REJECTED

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
