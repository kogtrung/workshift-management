package com.workshift.backend.me.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.ShiftStatus;

public record MyCalendarItemResponse(
		Long registrationId,
		Long groupId,
		String groupName,
		Long shiftId,
		String shiftName,
		LocalDate date,
		LocalTime startTime,
		LocalTime endTime,
		ShiftStatus shiftStatus,
		Long positionId,
		String positionName,
		String positionColorCode,
		RegistrationStatus registrationStatus
) {
}

