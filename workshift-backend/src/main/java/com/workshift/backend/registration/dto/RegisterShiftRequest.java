package com.workshift.backend.registration.dto;

import jakarta.validation.constraints.NotNull;

public class RegisterShiftRequest {

	@NotNull(message = "Position ID là bắt buộc")
	private Long positionId;

	private String note;

	public Long getPositionId() {
		return positionId;
	}

	public void setPositionId(Long positionId) {
		this.positionId = positionId;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
