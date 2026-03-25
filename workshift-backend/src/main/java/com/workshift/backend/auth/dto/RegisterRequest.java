package com.workshift.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "Username không được để trống")
		@Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
		String username,

		@NotBlank(message = "Email không được để trống")
		@Email(message = "Email không hợp lệ")
		String email,

		@NotBlank(message = "Mật khẩu không được để trống")
		@Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
		String password,

		@NotBlank(message = "Họ tên không được để trống")
		@Size(max = 255, message = "Họ tên tối đa 255 ký tự")
		String fullName,

		@Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
		String phone
) {
}
