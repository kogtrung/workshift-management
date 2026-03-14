package com.workshift.backend.group.dto;

import java.util.List;

public record GroupAuditMonthlySummaryResponse(
		Long groupId,
		int month,
		int year,
		long totalEvents,
		List<GroupAuditSummaryItem> byAction
) {
}
