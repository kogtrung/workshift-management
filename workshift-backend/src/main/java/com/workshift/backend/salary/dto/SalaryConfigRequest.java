package com.workshift.backend.salary.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SalaryConfigRequest {

    private Long positionId;
    private Long userId;

    @NotNull(message = "Mức lương giờ không được để trống")
    @DecimalMin(value = "0.0", message = "Mức lương giờ phải lớn hơn hoặc bằng 0")
    private BigDecimal hourlyRate;

    @NotNull(message = "Ngày áp dụng không được để trống")
    private LocalDate effectiveDate;

    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
}
