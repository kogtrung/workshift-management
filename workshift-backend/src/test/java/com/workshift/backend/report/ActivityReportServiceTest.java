package com.workshift.backend.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.report.dto.ActivityReportResponse;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ActivityReportServiceTest {

    @Autowired private ActivityReportService activityReportService;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private RegistrationRepository registrationRepository;

    private User manager;
    private User member1;
    private User member2;
    private Group group;
    private Position position;

    // Dùng tháng 1/2026 và tháng 2/2026 làm fixture cố định
    private static final int YEAR = 2026;
    private static final int MONTH = 1; // January 2026

    @BeforeEach
    void setUp() {
        manager = userRepository.save(makeUser("rpt_manager", "rpt_manager@test.com"));
        member1 = userRepository.save(makeUser("rpt_member1", "rpt_member1@test.com"));
        member2 = userRepository.save(makeUser("rpt_member2", "rpt_member2@test.com"));

        group = new Group();
        group.setName("Report Test Group");
        group.setCreatedBy(manager);
        group = groupRepository.save(group);

        saveMembership(group, manager, GroupRole.MANAGER);
        saveMembership(group, member1, GroupRole.MEMBER);
        saveMembership(group, member2, GroupRole.MEMBER);

        position = new Position();
        position.setGroup(group);
        position.setName("Pha chế");
        position = positionRepository.save(position);
    }

    // ===== Monthly report tests =====

    @Test
    void getMonthlyReport_success_correctStats() {
        // member1: 2 ca trong tháng 1 (each 4h = 8h total)
        addApprovedReg(member1, makeShift("Ca 1", LocalDate.of(2026, 1, 5), LocalTime.of(8, 0), LocalTime.of(12, 0)));
        addApprovedReg(member1, makeShift("Ca 2", LocalDate.of(2026, 1, 12), LocalTime.of(8, 0), LocalTime.of(12, 0)));
        // member2: 1 ca trong tháng 1 (6h)
        addApprovedReg(member2, makeShift("Ca 3", LocalDate.of(2026, 1, 10), LocalTime.of(8, 0), LocalTime.of(14, 0)));

        ActivityReportResponse report = activityReportService
                .getMonthlyReport(group.getId(), YEAR, MONTH, manager.getUsername());

        assertEquals("MONTHLY", report.periodType());
        assertEquals(3, report.totalShifts()); // 2 + 1
        assertEquals(0, report.totalHours().compareTo(new java.math.BigDecimal("14.00"))); // 8 + 6
        assertEquals(2, report.members().size());
    }

    @Test
    void getMonthlyReport_noPreviousData_nullDelta() {
        // Không có dữ liệu tháng 12/2025 (kỳ trước)
        addApprovedReg(member1, makeShift("Ca Jan", LocalDate.of(2026, 1, 5), LocalTime.of(8, 0), LocalTime.of(12, 0)));

        ActivityReportResponse report = activityReportService
                .getMonthlyReport(group.getId(), YEAR, MONTH, manager.getUsername());

        // prevTotalShifts = 0 → shiftsChangePct phải null
        assertNull(report.shiftsChangePct());
        assertNull(report.hoursChangePct());
        assertEquals(0, report.prevTotalShifts());
    }

    @Test
    void getMonthlyReport_withPreviousData_correctDelta() {
        // Tháng 12/2025 (kỳ trước): 2 ca
        addApprovedReg(member1, makeShift("Ca Dec1", LocalDate.of(2025, 12, 5), LocalTime.of(8, 0), LocalTime.of(10, 0)));
        addApprovedReg(member1, makeShift("Ca Dec2", LocalDate.of(2025, 12, 15), LocalTime.of(8, 0), LocalTime.of(10, 0)));
        // Tháng 1/2026 (kỳ hiện tại): 4 ca (tăng 100%)
        addApprovedReg(member1, makeShift("Ca Jan1", LocalDate.of(2026, 1, 5), LocalTime.of(8, 0), LocalTime.of(10, 0)));
        addApprovedReg(member1, makeShift("Ca Jan2", LocalDate.of(2026, 1, 12), LocalTime.of(8, 0), LocalTime.of(10, 0)));
        addApprovedReg(member1, makeShift("Ca Jan3", LocalDate.of(2026, 1, 19), LocalTime.of(8, 0), LocalTime.of(10, 0)));
        addApprovedReg(member1, makeShift("Ca Jan4", LocalDate.of(2026, 1, 26), LocalTime.of(8, 0), LocalTime.of(10, 0)));

        ActivityReportResponse report = activityReportService
                .getMonthlyReport(group.getId(), YEAR, MONTH, manager.getUsername());

        assertEquals(4, report.totalShifts());
        assertEquals(2, report.prevTotalShifts());
        assertNotNull(report.shiftsChangePct());
        assertEquals(100.0, report.shiftsChangePct(), 0.01); // tăng 100%
    }

    @Test
    void getMonthlyReport_emptyPeriod_returnsZeros() {
        ActivityReportResponse report = activityReportService
                .getMonthlyReport(group.getId(), YEAR, MONTH, manager.getUsername());

        assertEquals(0, report.totalShifts());
        assertEquals(0, report.totalHours().compareTo(java.math.BigDecimal.ZERO));
        assertEquals(0, report.activeMembers());
        assertTrue(report.members().isEmpty());
    }

    @Test
    void getMonthlyReport_fail_notManager() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                activityReportService.getMonthlyReport(group.getId(), YEAR, MONTH, member1.getUsername())
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void getMonthlyReport_fail_invalidMonth() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                activityReportService.getMonthlyReport(group.getId(), YEAR, 13, manager.getUsername())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ===== Weekly report tests =====

    @Test
    void getWeeklyReport_success() {
        // Tuần 2 của 2026: 5-Jan(Mon) to 11-Jan(Sun)
        addApprovedReg(member1, makeShift("Ca Tue", LocalDate.of(2026, 1, 6), LocalTime.of(8, 0), LocalTime.of(10, 0)));
        addApprovedReg(member1, makeShift("Ca Thu", LocalDate.of(2026, 1, 8), LocalTime.of(8, 0), LocalTime.of(10, 0)));

        ActivityReportResponse report = activityReportService
                .getWeeklyReport(group.getId(), 2026, 2, manager.getUsername());

        assertEquals("WEEKLY", report.periodType());
        assertEquals(2, report.totalShifts());
        assertEquals(0, report.totalHours().compareTo(new java.math.BigDecimal("4.00")));
    }

    @Test
    void getWeeklyReport_fail_invalidWeek() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                activityReportService.getWeeklyReport(group.getId(), 2026, 0, manager.getUsername())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ===== Helpers =====

    private User makeUser(String username, String email) {
        User u = new User();
        u.setUsername(username); u.setEmail(email);
        u.setPassword("password"); u.setFullName(username);
        return u;
    }

    private void saveMembership(Group g, User u, GroupRole role) {
        GroupMember gm = new GroupMember();
        gm.setGroup(g); gm.setUser(u);
        gm.setRole(role); gm.setStatus(GroupMemberStatus.APPROVED);
        groupMemberRepository.save(gm);
    }

    private Shift makeShift(String name, LocalDate date, LocalTime start, LocalTime end) {
        Shift s = new Shift();
        s.setGroup(group); s.setName(name); s.setDate(date);
        s.setStartTime(start); s.setEndTime(end);
        s.setStatus(ShiftStatus.OPEN);
        return shiftRepository.save(s);
    }

    private void addApprovedReg(User user, Shift shift) {
        Registration r = new Registration();
        r.setShift(shift); r.setUser(user); r.setPosition(position);
        r.setStatus(RegistrationStatus.APPROVED);
        registrationRepository.save(r);
    }
}
