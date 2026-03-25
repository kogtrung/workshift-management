package com.workshift.backend.group.dto;

import java.util.List;

public record GroupAuditLogPageResponse(
		List<GroupAuditLogResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages
) {
}
