package com.workshift.backend.shift.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateShiftBulkRequest(
		@NotEmpty(message = "Danh sách ca không được để trống")
		List<@Valid CreateShiftRequest> shifts
) {
}

