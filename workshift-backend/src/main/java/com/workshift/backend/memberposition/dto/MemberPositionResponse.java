package com.workshift.backend.memberposition.dto;

public record MemberPositionResponse(
		Long id,
		Long positionId,
		String positionName,
		String positionColorCode
) {
}
