package com.lms.auth_service.service.impl;

import com.lms.auth_service.dto.AuthResponseDto;
import com.lms.auth_service.dto.ProfileUpdateRequestDto;
import com.lms.auth_service.dto.RegisterRequestDto;
import com.lms.auth_service.entity.User;
import com.lms.auth_service.enums.ApprovalStatus;
import com.lms.auth_service.enums.AuthProvider;
import com.lms.auth_service.enums.Role;
import com.lms.auth_service.repository.UserRepository;
import com.lms.auth_service.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;
    private RegisterRequestDto registerRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1)
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .role(Role.STUDENT)
                .provider(AuthProvider.LOCAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        registerRequest = new RegisterRequestDto();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole("STUDENT");
        registerRequest.setGender("MALE");
    }

    // ==================== REGISTER TESTS ====================

    @Test
    void register_success() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        User result = authService.register(registerRequest);

        assertNotNull(result);
        assertEquals("John Doe", result.getFullName());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void register_emailAlreadyExists_throwsException() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));

        assertEquals("User with this email already exists!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void login_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", mockUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(anyInt(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn("mock-jwt-token");
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        AuthResponseDto result = authService.login("john@example.com", "password123");

        assertNotNull(result);
        assertEquals("mock-jwt-token", result.getToken());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("John Doe", result.getFullName());
        verify(userRepository, times(1)).findByEmail("john@example.com");
    }

    @Test
    void login_userNotFound_throwsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login("unknown@example.com", "password123"));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void login_accountSuspended_throwsException() {
        mockUser.setActive(false);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        assertThrows(RuntimeException.class,
                () -> authService.login("john@example.com", "password123"));
    }

    @Test
    void login_wrongPassword_throwsException() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPassword", mockUser.getPasswordHash())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login("john@example.com", "wrongPassword"));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void login_googleAccountNoPassword_throwsException() {
        mockUser.setPasswordHash(null);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        assertThrows(RuntimeException.class,
                () -> authService.login("john@example.com", "password123"));
    }

    @Test
    void login_adminNotFound_createsDefaultAdmin() {
        when(userRepository.findByEmail("admin123@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin@123")).thenReturn("hashedAdminPassword");

        User adminUser = User.builder()
                .userId(99)
                .email("admin123@gmail.com")
                .fullName("LMS Administrator")
                .passwordHash("hashedAdminPassword")
                .role(Role.ADMIN)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        when(passwordEncoder.matches("Admin@123", "hashedAdminPassword")).thenReturn(true);
        when(jwtUtil.generateToken(anyInt(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn("admin-jwt-token");
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        AuthResponseDto result = authService.login("admin123@gmail.com", "Admin@123");

        assertNotNull(result);
        assertEquals("admin123@gmail.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==================== GET USER TESTS ====================

    @Test
    void getUserByEmail_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        User result = authService.getUserByEmail("john@example.com");

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getUserByEmail_notFound_throwsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.getUserByEmail("unknown@example.com"));
    }

    @Test
    void getUserById_success() {
        when(userRepository.findByUserId(1)).thenReturn(mockUser);

        User result = authService.getUserById(1);

        assertNotNull(result);
        assertNull(result.getPasswordHash()); // password should be cleared
        assertEquals(1, result.getUserId());
    }

    @Test
    void getUserById_notFound_throwsException() {
        when(userRepository.findByUserId(99)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> authService.getUserById(99));
    }

    @Test
    void getUsersByIds_success() {
        List<User> users = List.of(mockUser);
        when(userRepository.findByUserIdIn(List.of(1))).thenReturn(users);

        List<User> result = authService.getUsersByIds(List.of(1));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getPasswordHash()); // password should be cleared
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Test
    void changePassword_success() {
        when(userRepository.findByUserId(1)).thenReturn(mockUser);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        authService.changePassword(1, "newPassword");

        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("newPassword");
    }

    @Test
    void changePassword_userNotFound_throwsException() {
        when(userRepository.findByUserId(99)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> authService.changePassword(99, "newPassword"));
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Test
    void updateProfile_success() {
        ProfileUpdateRequestDto updateDto = new ProfileUpdateRequestDto();
        updateDto.setFullName("John Updated");
        updateDto.setMobile(9999999999L);
        updateDto.setBio("Updated bio");
        updateDto.setProfilePicUrl("http://pic.url/new.jpg");
        updateDto.setGender("MALE");

        when(userRepository.findByUserId(1)).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User result = authService.updateProfile(1, updateDto);

        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateProfile_userNotFound_throwsException() {
        ProfileUpdateRequestDto updateDto = new ProfileUpdateRequestDto();
        when(userRepository.findByUserId(99)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> authService.updateProfile(99, updateDto));
    }

    // ==================== FORGOT / RESET PASSWORD TESTS ====================

    @Test
    void forgotPassword_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        authService.forgotPassword("john@example.com");

        verify(userRepository, times(1)).save(any(User.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void forgotPassword_emailNotFound_throwsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.forgotPassword("unknown@example.com"));
    }

    @Test
    void resetPassword_success() {
        mockUser.setResetOtp("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        authService.resetPassword("john@example.com", "123456", "newPassword");

        verify(userRepository, times(1)).save(any(User.class));
        assertNull(mockUser.getResetOtp());
        assertNull(mockUser.getOtpExpiry());
    }

    @Test
    void resetPassword_invalidOtp_throwsException() {
        mockUser.setResetOtp("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        assertThrows(RuntimeException.class,
                () -> authService.resetPassword("john@example.com", "999999", "newPassword"));
    }

    @Test
    void resetPassword_expiredOtp_throwsException() {
        mockUser.setResetOtp("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().minusMinutes(5)); // already expired
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        assertThrows(RuntimeException.class,
                () -> authService.resetPassword("john@example.com", "123456", "newPassword"));
    }

    // ==================== VERIFY OTP TESTS ====================

    @Test
    void verifyOtp_success() {
        mockUser.setResetOtp("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        boolean result = authService.verifyOtp("john@example.com", "123456");

        assertTrue(result);
    }

    @Test
    void verifyOtp_wrongOtp_returnsFalse() {
        mockUser.setResetOtp("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        boolean result = authService.verifyOtp("john@example.com", "000000");

        assertFalse(result);
    }

    @Test
    void verifyOtp_expiredOtp_returnsFalse() {
        mockUser.setResetOtp("123456");
        mockUser.setOtpExpiry(LocalDateTime.now().minusMinutes(5));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        boolean result = authService.verifyOtp("john@example.com", "123456");

        assertFalse(result);
    }

    // ==================== ADMIN ACTIONS TESTS ====================

    @Test
    void updateUserStatus_activateUser_success() {
        when(userRepository.findByUserId(1)).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        authService.updateUserStatus(1, true);

        assertTrue(mockUser.isActive());
        verify(userRepository, times(1)).save(any(User.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void updateUserStatus_suspendUser_success() {
        when(userRepository.findByUserId(1)).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        authService.updateUserStatus(1, false);

        assertFalse(mockUser.isActive());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUserStatus_userNotFound_doesNothing() {
        when(userRepository.findByUserId(99)).thenReturn(null);

        authService.updateUserStatus(99, true);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_success() {
        doNothing().when(userRepository).deleteById(1);

        authService.deleteUser(1);

        verify(userRepository, times(1)).deleteById(1);
    }

    @Test
    void updateUserRole_success() {
        when(userRepository.findByUserId(1)).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        authService.updateUserRole(1, "INSTRUCTOR");

        assertEquals(Role.INSTRUCTOR, mockUser.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void searchUsers_success() {
        when(userRepository.findByFullNameContaining("John")).thenReturn(List.of(mockUser));

        List<User> result = authService.searchUsers("John");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getFullName());
    }

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser));

        List<User> result = authService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
