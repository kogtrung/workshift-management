package com.workshift.backend.group.dto;

public record GroupAuditSummaryItem(
		String actionType,
		long count
) {
}
