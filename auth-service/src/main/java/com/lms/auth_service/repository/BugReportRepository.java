package com.lms.auth_service.repository;

import com.lms.auth_service.entity.BugReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BugReportRepository extends JpaRepository<BugReport, Integer> {
    List<BugReport> findAllByOrderByCreatedAtDesc();
}
