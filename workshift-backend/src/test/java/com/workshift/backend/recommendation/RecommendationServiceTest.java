package com.workshift.backend.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
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
import com.workshift.backend.domain.Availability;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.MemberPosition;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.recommendation.dto.CandidateResponse;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.AvailabilityRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.MemberPositionRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private MemberPositionRepository memberPositionRepository;
    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired
    private RegistrationRepository registrationRepository;

    private User manager;
    private User member;
    private Group group;
    private Position position;
    private Shift shift;
    private GroupMember memberGroupMember;

    // Ca sáng thứ Hai: 08:00 – 12:00
    private static final LocalDate SHIFT_DATE = LocalDate.of(2026, 3, 30); // Monday
    private static final DayOfWeek SHIFT_DAY = DayOfWeek.MONDAY;

    @BeforeEach
    void setUp() {
        // Manager
        manager = new User();
        manager.setUsername("rec_manager");
        manager.setEmail("rec_manager@test.com");
        manager.setPassword("password");
        manager.setFullName("Rec Manager");
        manager = userRepository.save(manager);

        // Member
        member = new User();
        member.setUsername("rec_member");
        member.setEmail("rec_member@test.com");
        member.setPassword("password");
        member.setFullName("Rec Member");
        member = userRepository.save(member);

        // Group
        group = new Group();
        group.setName("Rec Test Group");
        group.setCreatedBy(manager);
        group = groupRepository.save(group);

        // Manager membership
        GroupMember managerMembership = new GroupMember();
        managerMembership.setGroup(group);
        managerMembership.setUser(manager);
        managerMembership.setRole(GroupRole.MANAGER);
        managerMembership.setStatus(GroupMemberStatus.APPROVED);
        groupMemberRepository.save(managerMembership);

        // Member membership
        memberGroupMember = new GroupMember();
        memberGroupMember.setGroup(group);
        memberGroupMember.setUser(member);
        memberGroupMember.setRole(GroupRole.MEMBER);
        memberGroupMember.setStatus(GroupMemberStatus.APPROVED);
        memberGroupMember = groupMemberRepository.save(memberGroupMember);

        // Position
        position = new Position();
        position.setGroup(group);
        position.setName("Pha chế");
        position = positionRepository.save(position);

        // Shift thứ Hai 08:00-12:00
        shift = new Shift();
        shift.setGroup(group);
        shift.setName("Ca sáng");
        shift.setDate(SHIFT_DATE);
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(12, 0));
        shift.setStatus(ShiftStatus.OPEN);
        shift = shiftRepository.save(shift);
    }

    // ===== Helpers =====

    private void grantPosition(GroupMember gm, Position p) {
        MemberPosition mp = new MemberPosition();
        mp.setGroupMember(gm);
        mp.setPosition(p);
        memberPositionRepository.save(mp);
    }

    private void grantAvailability(User u, DayOfWeek day, LocalTime start, LocalTime end) {
        Availability avail = new Availability();
        avail.setUser(u);
        avail.setDayOfWeek(day);
        avail.setStartTime(start);
        avail.setEndTime(end);
        availabilityRepository.save(avail);
    }

    private void addApprovedRegistration(User u, Shift s, Position p) {
        Registration reg = new Registration();
        reg.setShift(s);
        reg.setUser(u);
        reg.setPosition(p);
        reg.setStatus(RegistrationStatus.APPROVED);
        registrationRepository.save(reg);
    }

    // ===== Tests =====

    @Test
    void recommend_success_allCriteriaMet() {
        // Rảnh + Đúng vị trí + Chưa có lịch → phải có trong kết quả
        grantPosition(memberGroupMember, position);
        grantAvailability(member, SHIFT_DAY, LocalTime.of(7, 0), LocalTime.of(13, 0));

        List<CandidateResponse> result = recommendationService
                .recommend(group.getId(), shift.getId(), position.getId(), manager.getUsername());

        assertEquals(1, result.size());
        assertEquals(member.getId(), result.get(0).userId());
        assertEquals(member.getFullName(), result.get(0).fullName());
        assertEquals(member.getUsername(), result.get(0).username());
    }

    @Test
    void recommend_notAvailable_excluded() {
        // Có vị trí nhưng không có Availability ngày đó → không có trong kết quả
        grantPosition(memberGroupMember, position);
        // Không add Availability → bị loại

        List<CandidateResponse> result = recommendationService
                .recommend(group.getId(), shift.getId(), position.getId(), manager.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void recommend_availabilityOutsideShiftTime_excluded() {
        // Availability trùng ngày nhưng không giao giờ ca (14:00-18:00 vs ca 08:00-12:00)
        grantPosition(memberGroupMember, position);
        grantAvailability(member, SHIFT_DAY, LocalTime.of(14, 0), LocalTime.of(18, 0));

        List<CandidateResponse> result = recommendationService
                .recommend(group.getId(), shift.getId(), position.getId(), manager.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void recommend_wrongPosition_excluded() {
        // Rảnh nhưng sai vị trí → không có trong kết quả
        grantAvailability(member, SHIFT_DAY, LocalTime.of(7, 0), LocalTime.of(13, 0));
        // Không grantPosition → bị loại

        List<CandidateResponse> result = recommendationService
                .recommend(group.getId(), shift.getId(), position.getId(), manager.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void recommend_alreadyScheduledSameDay_excluded() {
        // Đủ điều kiện nhưng đã có APPROVED registration cùng ngày → bị loại
        grantPosition(memberGroupMember, position);
        grantAvailability(member, SHIFT_DAY, LocalTime.of(7, 0), LocalTime.of(13, 0));

        // Tạo ca khác cùng ngày và APPROVED
        Shift otherShift = new Shift();
        otherShift.setGroup(group);
        otherShift.setName("Ca chiều");
        otherShift.setDate(SHIFT_DATE);
        otherShift.setStartTime(LocalTime.of(13, 0));
        otherShift.setEndTime(LocalTime.of(17, 0));
        otherShift.setStatus(ShiftStatus.OPEN);
        otherShift = shiftRepository.save(otherShift);

        addApprovedRegistration(member, otherShift, position);

        List<CandidateResponse> result = recommendationService
                .recommend(group.getId(), shift.getId(), position.getId(), manager.getUsername());

        assertTrue(result.isEmpty());
    }

    @Test
    void recommend_fail_notManager() {
        // Member thường gọi → 403 FORBIDDEN
        BusinessException ex = assertThrows(BusinessException.class, () ->
                recommendationService.recommend(group.getId(), shift.getId(), position.getId(), member.getUsername())
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Chỉ Quản lý mới có quyền xem gợi ý nhân viên", ex.getMessage());
    }
}
