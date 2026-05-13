package com.lms.auth_service.service.impl;

import com.lms.auth_service.entity.User;
import com.lms.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setFullName("John Doe");
        mockUser.setEmail("john@example.com");
    }

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser));

        List<User> result = adminUserService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getFullName());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_emptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = adminUserService.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void deleteUser_success() {
        doNothing().when(userRepository).deleteById(1);

        adminUserService.deleteUser(1);

        verify(userRepository, times(1)).deleteById(1);
    }

    @Test
    void deleteUser_nonExistentUser_noException() {
        doNothing().when(userRepository).deleteById(99);

        assertDoesNotThrow(() -> adminUserService.deleteUser(99));
        verify(userRepository, times(1)).deleteById(99);
    }
}
