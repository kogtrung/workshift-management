package com.workshift.backend.domain;

import com.workshift.backend.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shift_change_requests")
public class ShiftChangeRequest extends BaseEntity {

    /** Registration hiện tại của requester (phải APPROVED) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_registration_id", nullable = false)
    private Registration fromRegistration;

    /** Ca muốn đổi tới */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_shift_id", nullable = false)
    private Shift toShift;

    /** Vị trí muốn đăng ký ở ca mới */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_position_id", nullable = false)
    private Position toPosition;

    /** Người tạo request */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShiftChangeRequestStatus status = ShiftChangeRequestStatus.PENDING;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "manager_note", length = 500)
    private String managerNote;

    public Registration getFromRegistration() { return fromRegistration; }
    public void setFromRegistration(Registration fromRegistration) { this.fromRegistration = fromRegistration; }

    public Shift getToShift() { return toShift; }
    public void setToShift(Shift toShift) { this.toShift = toShift; }

    public Position getToPosition() { return toPosition; }
    public void setToPosition(Position toPosition) { this.toPosition = toPosition; }

    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }

    public ShiftChangeRequestStatus getStatus() { return status; }
    public void setStatus(ShiftChangeRequestStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getManagerNote() { return managerNote; }
    public void setManagerNote(String managerNote) { this.managerNote = managerNote; }
}
