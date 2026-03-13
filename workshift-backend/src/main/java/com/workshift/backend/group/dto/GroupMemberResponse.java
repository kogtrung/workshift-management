package com.workshift.backend.group.dto;

public record GroupMemberResponse(
		Long memberId,
		Long groupId,
		Long userId,
		String role,
		String status
) {
}
