package com.workshift.backend.availability.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(
		@NotNull(message = "Danh sách lịch rảnh không được để trống")
		List<@Valid AvailabilityItemRequest> items
) {
}
