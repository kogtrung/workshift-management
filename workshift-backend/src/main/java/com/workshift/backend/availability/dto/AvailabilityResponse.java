package com.workshift.backend.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityResponse(
		Long id,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime
) {
}
