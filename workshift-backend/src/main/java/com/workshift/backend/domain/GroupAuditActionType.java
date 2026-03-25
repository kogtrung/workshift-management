package com.workshift.backend.domain;

public enum GroupAuditActionType {
	GROUP_CREATED,
	GROUP_UPDATED,
	GROUP_CLOSED,
	GROUP_REOPENED,
	GROUP_DELETE,      // legacy – dữ liệu cũ trong DB
	GROUP_DELETED,     // legacy – dữ liệu cũ trong DB
	GROUP_MEMBER_JOIN_REQUESTED,
	GROUP_MEMBER_APPROVED,
	GROUP_MEMBER_REJECTED
}
