package com.workshift.backend.position.dto;

public record PositionResponse(
		Long id,
		Long groupId,
		String name,
		String colorCode
) {
}
