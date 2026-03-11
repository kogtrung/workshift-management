package com.workshift.backend.group.dto;

public record JoinGroupResponse(
		Long groupId,
		Long userId,
		String role,
		String status
) {
}
