package com.workshift.backend.auth.dto;

public record LoginResponse(
		String token,
		Long userId,
		String username,
		String email,
		String fullName
) {
}
