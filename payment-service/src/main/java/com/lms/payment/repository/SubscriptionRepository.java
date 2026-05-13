package com.lms.payment.repository;

import com.lms.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Subscription entity.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    // All subscriptions (any status) belonging to a student
    List<Subscription> findByStudentId(int studentId);

    // Finds the subscription for a student that matches a given status (e.g., ACTIVE)
    Optional<Subscription> findByStudentIdAndStatus(int studentId, String status);

    // Returns subscriptions whose end-date has passed — used by scheduled expiry jobs
    List<Subscription> findByEndDateBefore(LocalDate date);

    // Analytics: how many subscriptions exist per plan tier
    long countByPlan(String plan);
}
