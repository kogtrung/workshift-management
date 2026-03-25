package com.workshift.backend.shiftchange.dto;

public record CreateChangeRequestDto(
        Long fromRegistrationId,
        Long toShiftId,
        Long toPositionId,
        String reason
) {
}
