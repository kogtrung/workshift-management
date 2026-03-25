package com.workshift.backend.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.User;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.report.dto.ActivityReportResponse;
import com.workshift.backend.report.dto.MemberActivityStats;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class ActivityReportService {

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public ActivityReportService(
            RegistrationRepository registrationRepository,
            UserRepository userRepository,
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository) {
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    // =====================================================================
    // Báo cáo theo tuần (ISO week)
    // =====================================================================

    @Transactional(readOnly = true)
    public ActivityReportResponse getWeeklyReport(Long groupId, int year, int week, String username) {
        assertManager(groupId, username);

        if (week < 1 || week > 53) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tuần không hợp lệ (1–53)");
        }

        // Tính ngày đầu/cuối tuần hiện tại (ISO: tuần bắt đầu Thứ Hai)
        LocalDate weekStart = LocalDate.of(year, 1, 4) // 4-Jan luôn nằm trong tuần 1
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        // Tuần trước
        LocalDate prevStart = weekStart.minusWeeks(1);
        LocalDate prevEnd = weekEnd.minusWeeks(1);

        return buildReport("WEEKLY", groupId, weekStart, weekEnd, prevStart, prevEnd);
    }

    // =====================================================================
    // Báo cáo theo tháng
    // =====================================================================

    @Transactional(readOnly = true)
    public ActivityReportResponse getMonthlyReport(Long groupId, int year, int month, String username) {
        assertManager(groupId, username);

        if (month < 1 || month > 12) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tháng không hợp lệ (1–12)");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        YearMonth prevYm = ym.minusMonths(1);
        LocalDate prevFrom = prevYm.atDay(1);
        LocalDate prevTo = prevYm.atEndOfMonth();

        return buildReport("MONTHLY", groupId, from, to, prevFrom, prevTo);
    }

    // =====================================================================
    // Core builder
    // =====================================================================

    private ActivityReportResponse buildReport(
            String periodType,
            Long groupId,
            LocalDate from, LocalDate to,
            LocalDate prevFrom, LocalDate prevTo) {

        List<MemberActivityStats> current = computeStats(groupId, from, to);
        List<MemberActivityStats> previous = computeStats(groupId, prevFrom, prevTo);

        int totalShifts = current.stream().mapToInt(MemberActivityStats::totalShifts).sum();
        BigDecimal totalHours = current.stream()
                .map(MemberActivityStats::totalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int activeMembers = current.size();

        int prevTotalShifts = previous.stream().mapToInt(MemberActivityStats::totalShifts).sum();
        BigDecimal prevTotalHours = previous.stream()
                .map(MemberActivityStats::totalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int prevActiveMembers = previous.size();

        Double shiftsChangePct = computeChangePct(prevTotalShifts, totalShifts);
        Double hoursChangePct = computeChangePct(
                prevTotalHours.doubleValue(), totalHours.doubleValue());

        return new ActivityReportResponse(
                periodType, from, to,
                prevFrom, prevTo,
                totalShifts, totalHours, activeMembers,
                prevTotalShifts, prevTotalHours, prevActiveMembers,
                shiftsChangePct, hoursChangePct,
                current, previous
        );
    }

    /**
     * Tính stats per-member trong khoảng [from, to].
     * Dùng APPROVED registrations.
     */
    private List<MemberActivityStats> computeStats(Long groupId, LocalDate from, LocalDate to) {
        List<Registration> regs = registrationRepository
                .findApprovedByGroupAndDateRange(groupId, from, to);

        Map<Long, UserAccumulator> accMap = new LinkedHashMap<>();

        for (Registration reg : regs) {
            User u = reg.getUser();
            Shift shift = reg.getShift();
            long minutes = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
            BigDecimal hours = BigDecimal.valueOf(minutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            accMap.computeIfAbsent(u.getId(), id -> new UserAccumulator(u))
                    .add(hours);
        }

        return accMap.values().stream()
                .map(UserAccumulator::toStats)
                .toList();
    }

    /**
     * % thay đổi = (current - previous) / previous * 100
     * null nếu previous = 0
     */
    private Double computeChangePct(double prev, double current) {
        if (prev == 0) return null;
        return Math.round(((current - prev) / prev) * 10000.0) / 100.0;
    }

    // =====================================================================
    // Auth helper
    // =====================================================================

    private void assertManager(Long groupId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

        if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền xem báo cáo hoạt động");
        }
    }

    // =====================================================================
    // Inner accumulator
    // =====================================================================

    private static class UserAccumulator {
        final User user;
        int totalShifts = 0;
        BigDecimal totalHours = BigDecimal.ZERO;

        UserAccumulator(User user) {
            this.user = user;
        }

        void add(BigDecimal hours) {
            totalShifts++;
            totalHours = totalHours.add(hours);
        }

        MemberActivityStats toStats() {
            return new MemberActivityStats(
                    user.getId(),
                    user.getFullName(),
                    user.getUsername(),
                    totalShifts,
                    totalHours
            );
        }
    }
}
