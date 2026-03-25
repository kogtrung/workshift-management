package com.workshift.backend.report.dto;

import java.math.BigDecimal;

/**
 * Thống kê hoạt động của 1 nhân viên trong 1 kỳ.
 */
public record MemberActivityStats(
        Long userId,
        String fullName,
        String username,
        int totalShifts,
        BigDecimal totalHours
) {
}
