package com.workshift.backend.registration.dto;

import com.workshift.backend.domain.RegistrationStatus;

public class RegistrationResponse {

	private Long id;
	private Long shiftId;
	private Long userId;
	private Long positionId;
	private RegistrationStatus status;
	private String note;
	private String managerNote;

	public RegistrationResponse(Long id, Long shiftId, Long userId, Long positionId, RegistrationStatus status, String note, String managerNote) {
		this.id = id;
		this.shiftId = shiftId;
		this.userId = userId;
		this.positionId = positionId;
		this.status = status;
		this.note = note;
		this.managerNote = managerNote;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getShiftId() {
		return shiftId;
	}

	public void setShiftId(Long shiftId) {
		this.shiftId = shiftId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getPositionId() {
		return positionId;
	}

	public void setPositionId(Long positionId) {
		this.positionId = positionId;
	}

	public RegistrationStatus getStatus() {
		return status;
	}

	public void setStatus(RegistrationStatus status) {
		this.status = status;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getManagerNote() {
		return managerNote;
	}

	public void setManagerNote(String managerNote) {
		this.managerNote = managerNote;
	}
}
