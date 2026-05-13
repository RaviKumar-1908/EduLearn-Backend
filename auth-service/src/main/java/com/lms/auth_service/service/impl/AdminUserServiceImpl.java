package com.lms.auth_service.service.impl;

import com.lms.auth_service.entity.User;
import com.lms.auth_service.repository.UserRepository;
import com.lms.auth_service.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(int userId) {
        userRepository.deleteById(userId);
    }
}
