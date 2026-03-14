package com.workshift.backend.group.dto;

import java.time.LocalDate;
import java.util.List;

public record GroupAuditDailySummaryResponse(
		Long groupId,
		LocalDate date,
		long totalEvents,
		List<GroupAuditSummaryItem> byAction
) {
}
