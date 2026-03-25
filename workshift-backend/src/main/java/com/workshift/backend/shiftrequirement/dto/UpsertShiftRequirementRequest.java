package com.workshift.backend.shiftrequirement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertShiftRequirementRequest(
		@NotNull(message = "Vị trí không được để trống")
		Long positionId,

		@NotNull(message = "Số lượng không được để trống")
		@Min(value = 1, message = "Số lượng phải >= 1")
		Integer quantity
) {
}

