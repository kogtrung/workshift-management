package com.workshift.backend.group.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewGroupMemberRequest(
		@NotNull(message = "Action không được để trống")
		GroupMemberReviewAction action
) {
}
