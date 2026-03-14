package com.workshift.backend.group.dto;

import java.time.Instant;

public record GroupAuditLogResponse(
		Long id,
		Long groupId,
		Long actorUserId,
		String actorRole,
		String actionType,
		String entityType,
		Long entityId,
		Instant occurredAt,
		String summary,
		String beforeData,
		String afterData,
		String metadata
) {
}
