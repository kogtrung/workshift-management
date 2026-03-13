package com.workshift.backend.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinGroupByCodeRequest(
		@NotBlank(message = "Mã tham gia không được để trống")
		@Size(min = 6, max = 6, message = "Mã tham gia phải đúng 6 ký tự")
		@Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "Mã tham gia chỉ gồm chữ và số")
		String joinCode
) {
}
