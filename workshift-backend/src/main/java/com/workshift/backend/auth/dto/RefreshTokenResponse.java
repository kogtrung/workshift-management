package com.workshift.backend.auth.dto;

public record RefreshTokenResponse(
		String token,
		String refreshToken
) {
}
