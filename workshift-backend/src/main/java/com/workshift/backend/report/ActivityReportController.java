package com.workshift.backend.report;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.report.dto.ActivityReportResponse;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/reports")
public class ActivityReportController {

    private final ActivityReportService activityReportService;

    public ActivityReportController(ActivityReportService activityReportService) {
        this.activityReportService = activityReportService;
    }

    /**
     * B26: Báo cáo hiệu suất theo tuần (ISO week).
     * GET /api/v1/groups/{groupId}/reports/weekly?year=2026&week=13
     * Chỉ MANAGER.
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<ActivityReportResponse>> getWeeklyReport(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @RequestParam("year") int year,
            @RequestParam("week") int week
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        ActivityReportResponse data = activityReportService
                .getWeeklyReport(groupId, year, week, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok("Báo cáo hoạt động theo tuần", data));
    }

    /**
     * B26: Báo cáo hiệu suất theo tháng.
     * GET /api/v1/groups/{groupId}/reports/monthly?year=2026&month=3
     * Chỉ MANAGER.
     */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<ActivityReportResponse>> getMonthlyReport(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        ActivityReportResponse data = activityReportService
                .getMonthlyReport(groupId, year, month, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok("Báo cáo hoạt động theo tháng", data));
    }
}
