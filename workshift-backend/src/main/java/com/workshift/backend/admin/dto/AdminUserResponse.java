package com.workshift.backend.admin.dto;

import java.time.Instant;

import com.workshift.backend.domain.GlobalRole;
import com.workshift.backend.domain.UserStatus;

public record AdminUserResponse(
		Long id,
		String username,
		String email,
		String fullName,
		String phone,
		UserStatus status,
		GlobalRole globalRole,
		Instant createdAt
) {}
