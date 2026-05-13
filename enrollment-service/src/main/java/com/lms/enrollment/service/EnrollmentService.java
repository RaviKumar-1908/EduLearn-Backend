package com.lms.enrollment.service;

import com.lms.enrollment.entity.Enrollment;

import java.util.List;

public interface EnrollmentService {

    Enrollment enroll(int studentId, int courseId, double price);

    Enrollment enroll(int studentId, int courseId);

    void unenroll(int studentId, int courseId);

    List<Enrollment> getEnrollmentsByStudent(int studentId);

    List<Enrollment> getEnrollmentsByCourse(int courseId);

    void updateProgress(int studentId, int courseId, int progressPercent);

    void markComplete(int studentId, int courseId);

    boolean isEnrolled(int studentId, int courseId);

    void issueCertificate(int studentId, int courseId);

    int getEnrollmentCount(int courseId);

    java.util.Map<Integer, Integer> getEnrollmentCounts(java.util.List<Integer> courseIds);

    java.util.Map<String, Object> getAdminStats();
}
