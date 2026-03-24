package com.workshift.backend.position.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
		@NotBlank(message = "Tên vị trí không được để trống")
		@Size(max = 255, message = "Tên vị trí không được vượt quá 255 ký tự")
		String name,

		@Size(max = 50, message = "Mã màu không được vượt quá 50 ký tự")
		String colorCode
) {
}
