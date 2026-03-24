package com.workshift.backend.salary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SalaryConfigResponse {

    private Long id;
    private Long groupId;
    private Long positionId;
    private String positionName;
    private Long userId;
    private String userFullName;
    private BigDecimal hourlyRate;
    private LocalDate effectiveDate;

    public SalaryConfigResponse(Long id, Long groupId, Long positionId, String positionName, Long userId, String userFullName, BigDecimal hourlyRate, LocalDate effectiveDate) {
        this.id = id;
        this.groupId = groupId;
        this.positionId = positionId;
        this.positionName = positionName;
        this.userId = userId;
        this.userFullName = userFullName;
        this.hourlyRate = hourlyRate;
        this.effectiveDate = effectiveDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }

    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
}
