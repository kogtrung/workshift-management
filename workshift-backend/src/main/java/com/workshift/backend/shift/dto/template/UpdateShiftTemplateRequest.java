package com.workshift.backend.shift.dto.template;

import java.time.LocalTime;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateShiftTemplateRequest(
		@NotBlank(message = "Tên ca mẫu không được để trống")
		@Length(max = 255, message = "Tên ca mẫu không được vượt quá 255 ký tự")
		String name,

		@NotNull(message = "Giờ bắt đầu không được để trống")
		LocalTime startTime,

		@NotNull(message = "Giờ kết thúc không được để trống")
		LocalTime endTime,

		@Length(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
		String description
) {
}
