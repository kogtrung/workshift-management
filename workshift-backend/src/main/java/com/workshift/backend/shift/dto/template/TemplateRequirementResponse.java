package com.workshift.backend.shift.dto.template;

public record TemplateRequirementResponse(
		Long id,
		Long positionId,
		String positionName,
		String positionColorCode,
		int quantity
) {
}
