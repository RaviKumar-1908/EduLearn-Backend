package com.lms.progress.entity;

import jakarta.persistence.*;
// import lombok.AllArgsConstructor;
import lombok.Data;
// import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "certificate", uniqueConstraints = {
        @UniqueConstraint(name = "uq_student_course_cert", columnNames = { "student_id", "course_id" }),
        @UniqueConstraint(name = "uq_verification_code", columnNames = { "verification_code" })
})
@Data
public class Certificate {

    public Certificate() {
    }

    public Certificate(int certificateId, int studentId, int courseId, LocalDate issuedAt, String certificateUrl,
            String verificationCode, String instructorName, String courseName, String courseLevel,
            Integer courseDuration) {
        this.certificateId = certificateId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.issuedAt = issuedAt;
        this.certificateUrl = certificateUrl;
        this.verificationCode = verificationCode;
        this.instructorName = instructorName;
        this.courseName = courseName;
        this.courseLevel = courseLevel;
        this.courseDuration = courseDuration;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    private int certificateId;

    @Column(name = "student_id", nullable = false)
    private int studentId;

    @Column(name = "course_id", nullable = false)
    private int courseId;

    @Column(name = "issued_at", nullable = false)
    private LocalDate issuedAt;

    @Column(name = "certificate_url", nullable = false, length = 512)
    private String certificateUrl;

    @Column(name = "verification_code", nullable = false, unique = true, length = 64)
    private String verificationCode;

    @Column(name = "instructor_name", nullable = false, length = 150)
    private String instructorName;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Column(name = "course_level", length = 50)
    private String courseLevel;

    @Column(name = "course_duration")
    private Integer courseDuration;
}
