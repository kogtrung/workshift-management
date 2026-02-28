package com.workshift.backend.common.api;

import java.time.Instant;

public record ApiResponse<T>(
		int status,
		String message,
		T data,
		Instant timestamp
) {
	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(200, message, data, Instant.now());
	}

	public static <T> ApiResponse<T> created(String message, T data) {
		return new ApiResponse<>(201, message, data, Instant.now());
	}
}
