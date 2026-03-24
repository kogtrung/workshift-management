package com.workshift.backend.group.dto;

public record MyGroupResponse(
		Long groupId,
		String groupName,
		String description,
		String joinCode,
		String groupStatus,
		String myRole,
		String myMemberStatus
) {
}
