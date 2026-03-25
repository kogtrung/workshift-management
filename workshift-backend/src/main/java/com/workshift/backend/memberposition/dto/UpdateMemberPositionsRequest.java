package com.workshift.backend.memberposition.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberPositionsRequest(
		@NotNull(message = "Danh sách vị trí không được để trống")
		java.util.List<Long> positionIds
) {
}
