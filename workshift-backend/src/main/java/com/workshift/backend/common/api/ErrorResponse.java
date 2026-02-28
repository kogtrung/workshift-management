package com.workshift.backend.common.api;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
		int status,
		String message,
		Map<String, String> errors,
		String path,
		Instant timestamp
) {
	public static ErrorResponse of(int status, String message, Map<String, String> errors, String path) {
		return new ErrorResponse(status, message, errors, path, Instant.now());
	}
}
