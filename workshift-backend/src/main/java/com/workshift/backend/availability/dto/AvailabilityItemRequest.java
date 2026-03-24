package com.workshift.backend.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record AvailabilityItemRequest(
		@NotNull(message = "Ngày trong tuần không được để trống")
		DayOfWeek dayOfWeek,

		@NotNull(message = "Giờ bắt đầu không được để trống")
		LocalTime startTime,

		@NotNull(message = "Giờ kết thúc không được để trống")
		LocalTime endTime
) {
}
