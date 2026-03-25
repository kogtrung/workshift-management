package com.workshift.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.workshift.backend.domain.AdminAuditLog;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
