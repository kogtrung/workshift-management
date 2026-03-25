package com.workshift.backend.auth.dto;

public record RegisterResponse(
		Long id,
		String username,
		String email,
		String fullName
) {
}
