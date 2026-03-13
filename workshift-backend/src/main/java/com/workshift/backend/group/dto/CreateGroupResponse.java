package com.workshift.backend.group.dto;

public record CreateGroupResponse(
		Long id,
		String name,
		String description,
		String joinCode,
		String status,
		Long createdByUserId
) {
}
