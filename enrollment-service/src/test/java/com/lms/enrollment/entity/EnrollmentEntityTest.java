package com.lms.enrollment.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class EnrollmentEntityTest {

    @Test
    void testEnrollmentGettersSetters() {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(1);
        enrollment.setStudentId(10);
        enrollment.setCourseId(101);
        enrollment.setStatus("Active");
        enrollment.setEnrolledAt(LocalDate.now());
        enrollment.setProgressPercent(50);
        enrollment.setCertificateIssued(true);
        enrollment.setPriceAtPurchase(49.99);

        assertEquals(1, enrollment.getEnrollmentId());
        assertEquals(10, enrollment.getStudentId());
        assertEquals(101, enrollment.getCourseId());
        assertEquals("Active", enrollment.getStatus());
        assertNotNull(enrollment.getEnrolledAt());
        assertEquals(50, enrollment.getProgressPercent());
        assertTrue(enrollment.isCertificateIssued());
        assertEquals(49.99, enrollment.getPriceAtPurchase());
    }

    @Test
    void testConstructor() {
        Enrollment enrollment = new Enrollment(10, 101);
        assertEquals(10, enrollment.getStudentId());
        assertEquals(101, enrollment.getCourseId());
    }
}
