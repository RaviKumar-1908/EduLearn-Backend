package com.lms.payment.repository;

import com.lms.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity.
 * Spring Data JPA auto-generates SQL implementations for all these methods.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // Retrieves all payments made by a particular student
    List<Payment> findByStudentId(int studentId);

    // Retrieves all payments associated with a specific course
    List<Payment> findByCourseId(int courseId);

    // Filters payments by their current status (PENDING, SUCCESS, etc.)
    List<Payment> findByStatus(String status);

    // Looks up a payment by the external gateway transaction ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Aggregates the total amount paid by a student across all transactions
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.studentId = :studentId AND p.status = 'SUCCESS'")
    Double sumAmountByStudentId(@Param("studentId") int studentId);
}
