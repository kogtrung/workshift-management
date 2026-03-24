package com.workshift.backend.group.dto;

import java.time.Instant;

public record GroupMemberDetailResponse(
		Long memberId,
		Long groupId,
		Long userId,
		String username,
		String fullName,
		String email,
		String role,
		String status,
		Instant joinedAt
) {
}
