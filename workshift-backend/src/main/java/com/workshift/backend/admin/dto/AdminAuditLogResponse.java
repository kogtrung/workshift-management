package com.workshift.backend.admin.dto;

import java.time.Instant;

public record AdminAuditLogResponse(
		Long id,
		String adminUsername,
		String action,
		String target,
		String detail,
		Instant createdAt
) {}
