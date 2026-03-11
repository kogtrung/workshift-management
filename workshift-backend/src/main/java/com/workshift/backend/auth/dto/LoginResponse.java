package com.workshift.backend.auth.dto;

public record LoginResponse(
		String token,
		String refreshToken,
		Long userId,
		String username,
		String email,
		String fullName
) {
}
