package com.workshift.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.workshift.backend.domain.GroupAuditLog;

public interface GroupAuditLogRepository extends JpaRepository<GroupAuditLog, Long>, JpaSpecificationExecutor<GroupAuditLog> {
	void deleteAllByGroupId(Long groupId);

	long countByGroupId(Long groupId);
}
