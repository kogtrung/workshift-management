package com.workshift.backend.registration.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectRegistrationRequest {

	@NotBlank(message = "Vui lòng nhập lý do từ chối")
	private String reason;

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
