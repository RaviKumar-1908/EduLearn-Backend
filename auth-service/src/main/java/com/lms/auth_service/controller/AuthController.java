package com.lms.auth_service.controller;

import com.lms.auth_service.dto.ForgotPasswordRequestDto;
import com.lms.auth_service.dto.AuthResponseDto;
import com.lms.auth_service.dto.LoginRequestDto;
import com.lms.auth_service.dto.RegisterRequestDto;
import com.lms.auth_service.dto.ResetPasswordRequestDto;
import com.lms.auth_service.entity.User;
import com.lms.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequestDto request) {
        User registeredUser = authService.register(request);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto credentials) {
        AuthResponseDto response = authService.login(credentials.getEmail(), credentials.getPassword());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestHeader("Authorization") String token) {
        String newToken = authService.refreshToken(token);
        return new ResponseEntity<>(newToken, HttpStatus.OK);
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getProfile(@PathVariable int userId) {
        User user = authService.getUserById(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/profile/bulk")
    public ResponseEntity<java.util.List<User>> getProfiles(@RequestParam java.util.List<Integer> userIds) {
        return ResponseEntity.ok(authService.getUsersByIds(userIds));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<User> updateProfile(@PathVariable int userId, @RequestBody com.lms.auth_service.dto.ProfileUpdateRequestDto request) {
        User updatedUser = authService.updateProfile(userId, request);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @PutMapping("/password/{userId}")
    public ResponseEntity<Void> changePassword(@PathVariable int userId, @RequestBody Map<String, String> passwords) {
        authService.changePassword(userId, passwords.get("newPassword"));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.forgotPassword(request.getEmail());
        return new ResponseEntity<>("If the email exists, a reset link will be sent.", HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return new ResponseEntity<>("Password successfully reset.", HttpStatus.OK);
    }
    
    @PostMapping("/verify-otp")
    public ResponseEntity<Boolean> verifyOtp(@RequestBody Map<String, String> request) {
        boolean isValid = authService.verifyOtp(request.get("email"), request.get("otp"));
        if (!isValid) {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/delete/{userId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable int userId) {
        authService.deleteUser(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // --- Admin Management ---
    @GetMapping("/admin/users")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/admin/users/search")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<User>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(authService.searchUsers(query));
    }

    @PutMapping("/admin/users/{userId}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable int userId, @RequestParam boolean active) {
        authService.updateUserStatus(userId, active);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/admin/users/{userId}/role")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateRole(@PathVariable int userId, @RequestParam String role) {
        authService.updateUserRole(userId, role);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/ids")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<Integer>> getAdminIds() {
        return ResponseEntity.ok(authService.getAllAdminIds());
    }
}
