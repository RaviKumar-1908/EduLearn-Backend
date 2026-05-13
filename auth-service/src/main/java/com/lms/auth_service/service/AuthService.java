package com.lms.auth_service.service;

import com.lms.auth_service.dto.AuthResponseDto;
import com.lms.auth_service.dto.RegisterRequestDto;
import com.lms.auth_service.entity.User;

public interface AuthService {
    User register(RegisterRequestDto request);
    AuthResponseDto login(String email, String password);
    void logout(String token);
    boolean validateToken(String token);
    String refreshToken(String token);
    User getUserByEmail(String email);
    User getUserById(int userId);
    java.util.List<User> getUsersByIds(java.util.List<Integer> userIds);
    void changePassword(int userId, String newPassword);
    User updateProfile(int userId, com.lms.auth_service.dto.ProfileUpdateRequestDto user);
    
    // Admin methods
    java.util.List<User> getAllUsers();
    void updateUserStatus(int userId, boolean active);
    void deleteUser(int userId);
    void updateUserRole(int userId, String role);
    void forgotPassword(String email);
    void resetPassword(String email, String otp, String newPassword);
    boolean verifyOtp(String email, String otp);
    java.util.List<User> searchUsers(String query);
    java.util.List<Integer> getAllAdminIds();
}
