package com.lms.progress.repository;

import com.lms.progress.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Certificate} entities.
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Integer> {

    /**
     * Returns all certificates earned by a student across all courses.
     */
    List<Certificate> findByStudentId(int studentId);

    /**
     * Returns the certificate for a specific student / course pair, if it exists.
     */
    Optional<Certificate> findByStudentIdAndCourseId(int studentId, int courseId);

    /**
     * Looks up a certificate by its unique verification code.
     * Used by the public verification endpoint.
     */
    Optional<Certificate> findByVerificationCode(String verificationCode);

    /**
     * Checks whether a certificate has already been issued for this
     * student/course combination, avoiding duplicate issuance.
     */
    boolean existsByStudentIdAndCourseId(int studentId, int courseId);
}
