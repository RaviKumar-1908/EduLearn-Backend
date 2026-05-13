package com.lms.auth_service.controller;

import com.lms.auth_service.entity.BugReport;
import com.lms.auth_service.entity.User;
import com.lms.auth_service.enums.Role;
import com.lms.auth_service.repository.BugReportRepository;
import com.lms.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bugs")
@RequiredArgsConstructor
public class BugReportController {

    private final BugReportRepository bugReportRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping
    public ResponseEntity<BugReport> reportBug(@RequestBody BugReport bugReport) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = auth.getName();
            userRepository.findByEmail(email).ifPresent(user -> {
                if (bugReport.getUserId() == null) bugReport.setUserId(user.getUserId());
                if (bugReport.getUsername() == null) bugReport.setUsername(user.getFullName());
                if (bugReport.getEmail() == null) bugReport.setEmail(user.getEmail());
                bugReport.setReporterRole(user.getRole().name());
            });
        }

        if (bugReport.getEmail() == null || bugReport.getEmail().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (bugReport.getUsername() == null || bugReport.getUsername().isBlank()) {
            bugReport.setUsername("Guest User");
        }

        bugReport.setCreatedAt(LocalDateTime.now());
        bugReport.setStatus("OPEN");
        if (bugReport.getPriority() == null) bugReport.setPriority("MEDIUM");
        if (bugReport.getCategory() == null) bugReport.setCategory("GENERAL");

        BugReport saved = bugReportRepository.save(bugReport);

        // Send confirmation email to user
        Map<String, Object> event = new HashMap<>();
        event.put("userId", saved.getUserId());
        event.put("email", saved.getEmail());
        event.put("title", "🐞 Bug Report Received");
        event.put("message", "Hello " + saved.getUsername() + ", we have received your bug report regarding '" + saved.getCategory() + "'. Our team is investigating. Reference ID: #" + saved.getId());
        event.put("type", "BUG_REPORT_CONFIRMATION");
        
        rabbitTemplate.convertAndSend("lms.events.exchange", "notification.auth.bug", event);
        
        // Notify All Admins about the new bug report
        userRepository.findAllByRole(Role.ADMIN).forEach(admin -> {
            Map<String, Object> adminEvent = new HashMap<>();
            adminEvent.put("userId", admin.getUserId());
            adminEvent.put("email", admin.getEmail());
            adminEvent.put("title", "🐞 New Bug Report: " + saved.getCategory());
            adminEvent.put("message", saved.getUsername() + " reported a " + saved.getPriority() + " priority bug: " + 
                          (saved.getContent().length() > 80 ? saved.getContent().substring(0, 77) + "..." : saved.getContent()));
            adminEvent.put("type", "ADMIN_ALERT");
            adminEvent.put("relatedEntityId", saved.getId());
            adminEvent.put("relatedEntityType", "BUG_REPORT");
            
            rabbitTemplate.convertAndSend("lms.events.exchange", "notification.auth.bug", adminEvent);
        });

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BugReport>> getAllBugs() {
        return ResponseEntity.ok(bugReportRepository.findAllByOrderByCreatedAtDesc());
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BugReport> updateBug(@PathVariable Integer id, @RequestBody BugReport details) {
        BugReport bug = bugReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bug report not found"));
        
        if (details.getStatus() != null) bug.setStatus(details.getStatus());
        if (details.getPriority() != null) bug.setPriority(details.getPriority());
        if (details.getCategory() != null) bug.setCategory(details.getCategory());
        if (details.getAdminRemarks() != null) bug.setAdminRemarks(details.getAdminRemarks());
        
        bug.setUpdatedAt(LocalDateTime.now());
        BugReport updated = bugReportRepository.save(bug);

        String status = updated.getStatus();
        if ("RESOLVED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status)) {
            // Send email to user that the bug was updated
            Map<String, Object> event = new HashMap<>();
            event.put("email", updated.getEmail());
            event.put("userId", updated.getUserId());
            event.put("title", "✅ Bug Report Update: " + status);
            event.put("message", "Hello " + updated.getUsername() + ", your bug report #" + updated.getId() + " is now " + status + ". Admin Remarks: " + (updated.getAdminRemarks() != null ? updated.getAdminRemarks() : "Thank you for your report."));
            event.put("type", "BUG_REPORT_UPDATE");
            rabbitTemplate.convertAndSend("lms.events.exchange", "notification.auth.bug", event);
        }

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBug(@PathVariable Integer id) {
        if (!bugReportRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        bugReportRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
