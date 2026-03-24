package com.workshift.backend.shiftrequirement.dto;

public record ShiftRequirementResponse(
		Long id,
		Long shiftId,
		Long positionId,
		String positionName,
		String positionColorCode,
		Integer quantity
) {
}

