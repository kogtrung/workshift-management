package com.workshift.backend.shiftchange.dto;

import com.workshift.backend.domain.ShiftChangeRequestStatus;

public record ShiftChangeRequestResponse(
        Long id,
        Long fromRegistrationId,
        Long fromShiftId,
        Long toShiftId,
        Long toPositionId,
        String toPositionName,
        String requesterUsername,
        String requesterFullName,
        ShiftChangeRequestStatus status,
        String reason,
        String managerNote
) {
}
