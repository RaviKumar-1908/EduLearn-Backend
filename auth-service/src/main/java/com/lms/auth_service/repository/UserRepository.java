package com.lms.auth_service.repository;

import com.lms.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    User findByUserId(int userId);
    List<User> findByUserIdIn(List<Integer> userIds);
    boolean existsByEmail(String email);
    List<User> findAllByRole(com.lms.auth_service.enums.Role role);
    void deleteByUserId(int userId);
    List<User> findByFullNameContaining(String fullName);
}
