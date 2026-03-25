package com.workshift.backend.registration.dto;

import jakarta.validation.constraints.NotNull;

public class AssignShiftRequest {
    @NotNull(message = "Nhân viên không được để trống")
    private Long userId;

    @NotNull(message = "Vị trí không được để trống")
    private Long positionId;

    private String note;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
