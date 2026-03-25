package com.workshift.backend.registration.dto;

public class CancelRegistrationRequest {
    private String reason;

    public CancelRegistrationRequest() {}

    public CancelRegistrationRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
