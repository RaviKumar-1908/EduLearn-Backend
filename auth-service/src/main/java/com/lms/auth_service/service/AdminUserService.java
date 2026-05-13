package com.lms.auth_service.service;

import com.lms.auth_service.entity.User;
import java.util.List;

public interface AdminUserService {
    List<User> getAllUsers();
    void deleteUser(int userId);
}
