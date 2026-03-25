package com.workshift.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank(message = "Email/Username không được để trống")
		String usernameOrEmail,

		@NotBlank(message = "Mật khẩu không được để trống")
		String password
) {
}
