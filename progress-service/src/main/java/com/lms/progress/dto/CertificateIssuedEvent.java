package com.lms.progress.dto;

// import lombok.AllArgsConstructor;
import lombok.Data;
// import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
public class CertificateIssuedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public CertificateIssuedEvent() {
    }

    public CertificateIssuedEvent(int studentId, int courseId, String verificationCode, String certificateUrl,
            String courseName, String instructorName, String courseLevel, Integer courseDuration) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.verificationCode = verificationCode;
        this.certificateUrl = certificateUrl;
        this.courseName = courseName;
        this.instructorName = instructorName;
        this.courseLevel = courseLevel;
        this.courseDuration = courseDuration;
    }

    private int studentId;
    private int courseId;
    private String verificationCode;
    private String certificateUrl;
    private String courseName;
    private String instructorName;
    private String courseLevel;
    private Integer courseDuration;
}
