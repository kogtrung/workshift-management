package com.workshift.backend.shift.dto.template;

import java.time.LocalTime;

public record ShiftTemplateResponse(
		Long id,
		Long groupId,
		String name,
		LocalTime startTime,
		LocalTime endTime,
		String description
) {
}
