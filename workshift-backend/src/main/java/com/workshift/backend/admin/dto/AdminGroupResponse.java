package com.workshift.backend.admin.dto;

import java.time.Instant;

import com.workshift.backend.domain.GroupStatus;

public record AdminGroupResponse(
		Long id,
		String name,
		String joinCode,
		String description,
		Long createdById,
		String createdByUsername,
		GroupStatus status,
		Instant createdAt
) {}
