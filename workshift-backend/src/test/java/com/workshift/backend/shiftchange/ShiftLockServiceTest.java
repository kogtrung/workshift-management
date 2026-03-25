package com.workshift.backend.shiftchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shift.ShiftService;
import com.workshift.backend.shift.dto.CreateShiftResponse;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShiftLockServiceTest {

    @Autowired private ShiftService shiftService;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private RegistrationRepository registrationRepository;

    private User manager;
    private User member;
    private Group group;
    private Shift openShift;
    private Position position;

    @BeforeEach
    void setUp() {
        manager = userRepository.save(makeUser("lock_manager", "lock_manager@test.com"));
        member = userRepository.save(makeUser("lock_member", "lock_member@test.com"));

        group = new Group();
        group.setName("Lock Test Group");
        group.setCreatedBy(manager);
        group = groupRepository.save(group);

        saveMembership(group, manager, GroupRole.MANAGER);
        saveMembership(group, member, GroupRole.MEMBER);

        position = new Position();
        position.setGroup(group);
        position.setName("Pha chế");
        position = positionRepository.save(position);

        openShift = makeShift(group, "Ca sáng", LocalDate.now().plusDays(1), ShiftStatus.OPEN);
    }

    // ===== Tests =====

    @Test
    void lockShift_success_statusChangedAndPendingRejected() {
        // Tạo 1 PENDING registration
        registrationRepository.save(makePendingReg(openShift, member, position));

        CreateShiftResponse result = shiftService.lockShift(group.getId(), openShift.getId(), manager.getUsername());

        assertEquals(ShiftStatus.LOCKED, result.getStatus());

        // PENDING phải bị REJECTED
        Registration reg = registrationRepository.findByShiftIdAndStatus(openShift.getId(), RegistrationStatus.PENDING).stream().findFirst().orElse(null);
        assert reg == null; // không còn PENDING nào

        // Có 1 REJECTED
        assertEquals(1, registrationRepository.findByShiftIdAndStatus(openShift.getId(), RegistrationStatus.REJECTED).size());
    }

    @Test
    void lockShift_noRegistrations_success() {
        // Không có registration nào, vẫn lock được
        CreateShiftResponse result = shiftService.lockShift(group.getId(), openShift.getId(), manager.getUsername());
        assertEquals(ShiftStatus.LOCKED, result.getStatus());
    }

    @Test
    void lockShift_fail_notManager() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                shiftService.lockShift(group.getId(), openShift.getId(), member.getUsername())
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void lockShift_fail_alreadyLocked() {
        Shift lockedShift = makeShift(group, "Ca chiều", LocalDate.now().plusDays(1), ShiftStatus.LOCKED);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                shiftService.lockShift(group.getId(), lockedShift.getId(), manager.getUsername())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ===== Helpers =====

    private User makeUser(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("password");
        u.setFullName(username);
        return u;
    }

    private void saveMembership(Group g, User u, GroupRole role) {
        GroupMember gm = new GroupMember();
        gm.setGroup(g); gm.setUser(u);
        gm.setRole(role); gm.setStatus(GroupMemberStatus.APPROVED);
        groupMemberRepository.save(gm);
    }

    private Shift makeShift(Group g, String name, LocalDate date, ShiftStatus status) {
        Shift s = new Shift();
        s.setGroup(g); s.setName(name); s.setDate(date);
        s.setStartTime(LocalTime.of(8, 0)); s.setEndTime(LocalTime.of(12, 0));
        s.setStatus(status);
        return shiftRepository.save(s);
    }

    private Registration makePendingReg(Shift shift, User user, Position pos) {
        Registration r = new Registration();
        r.setShift(shift); r.setUser(user); r.setPosition(pos);
        r.setStatus(RegistrationStatus.PENDING);
        return r;
    }
}
