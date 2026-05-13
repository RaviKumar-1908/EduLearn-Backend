package com.lms.enrollment.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "enrollments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"studentId", "courseId"})
})
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int enrollmentId;

    @Column(nullable = false)
    private int studentId;

    @Column(nullable = false)
    private int courseId;

    @Column(nullable = false)
    private LocalDate enrolledAt;

    private LocalDate completedAt;

    @Column(nullable = false)
    private String status; // Active, Completed, Cancelled

    @Column(nullable = false)
    private int progressPercent;

    @Column(nullable = false)
    private boolean certificateIssued;

    @Column(nullable = false)
    private double priceAtPurchase;

    public Enrollment() {
        this.enrolledAt = LocalDate.now();
        this.status = "Active";
        this.progressPercent = 0;
        this.certificateIssued = false;
        this.priceAtPurchase = 0.0;
    }

    public Enrollment(int studentId, int courseId) {
        this();
        this.studentId = studentId;
        this.courseId = courseId;
    }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public LocalDate getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDate enrolledAt) { this.enrolledAt = enrolledAt; }

    public LocalDate getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDate completedAt) { this.completedAt = completedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public boolean isCertificateIssued() { return certificateIssued; }
    public void setCertificateIssued(boolean certificateIssued) { this.certificateIssued = certificateIssued; }

    public double getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(double priceAtPurchase) { this.priceAtPurchase = priceAtPurchase; }

    @Override
    public String toString() {
        return "Enrollment{" +
                "enrollmentId=" + enrollmentId +
                ", studentId=" + studentId +
                ", courseId=" + courseId +
                ", enrolledAt=" + enrolledAt +
                ", completedAt=" + completedAt +
                ", status='" + status + '\'' +
                ", progressPercent=" + progressPercent +
                ", certificateIssued=" + certificateIssued +
                '}';
    }
}
