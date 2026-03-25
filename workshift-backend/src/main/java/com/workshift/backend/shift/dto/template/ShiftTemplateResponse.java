package com.workshift.backend.shift.dto.template;

import java.time.LocalTime;
import java.util.List;

public record ShiftTemplateResponse(
		Long id,
		Long groupId,
		String name,
		LocalTime startTime,
		LocalTime endTime,
		String description,
		List<TemplateRequirementResponse> requirements
) {
}

