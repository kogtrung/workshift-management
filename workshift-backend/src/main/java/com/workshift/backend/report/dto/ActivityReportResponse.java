package com.workshift.backend.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response báo cáo hoạt động theo tuần hoặc tháng.
 * Bao gồm kỳ hiện tại và kỳ trước để so sánh hiệu suất.
 */
public record ActivityReportResponse(

        // Metadata kỳ báo cáo
        String periodType,       // "WEEKLY" hoặc "MONTHLY"
        LocalDate periodFrom,
        LocalDate periodTo,

        // Kỳ trước (để so sánh)
        LocalDate previousFrom,
        LocalDate previousTo,

        // Tổng hợp kỳ hiện tại
        int totalShifts,
        BigDecimal totalHours,
        int activeMembers,        // số thành viên tham gia ít nhất 1 ca

        // Tổng hợp kỳ trước
        int prevTotalShifts,
        BigDecimal prevTotalHours,
        int prevActiveMembers,

        // Delta % so sánh (null nếu kỳ trước = 0)
        Double shiftsChangePct,
        Double hoursChangePct,

        // Chi tiết từng thành viên kỳ hiện tại
        List<MemberActivityStats> members,

        // Chi tiết từng thành viên kỳ trước
        List<MemberActivityStats> previousMembers
) {
}
