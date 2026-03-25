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
import com.workshift.backend.domain.ShiftChangeRequestStatus;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shiftchange.dto.CreateChangeRequestDto;
import com.workshift.backend.shiftchange.dto.ShiftChangeRequestResponse;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShiftChangeServiceTest {

    @Autowired private ShiftChangeService shiftChangeService;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private RegistrationRepository registrationRepository;

    private User manager;
    private User member;
    private Group group;
    private Position position;
    private Shift fromShift;
    private Shift toShift;
    private Registration approvedReg; // member's APPROVED on fromShift

    @BeforeEach
    void setUp() {
        manager = userRepository.save(makeUser("chg_manager", "chg_manager@test.com"));
        member = userRepository.save(makeUser("chg_member", "chg_member@test.com"));

        group = new Group();
        group.setName("Change Test Group");
        group.setCreatedBy(manager);
        group = groupRepository.save(group);

        saveMembership(group, manager, GroupRole.MANAGER);
        saveMembership(group, member, GroupRole.MEMBER);

        position = new Position();
        position.setGroup(group);
        position.setName("Phục vụ");
        position = positionRepository.save(position);

        fromShift = makeShift(group, "Ca gốc", LocalDate.now().plusDays(2), ShiftStatus.OPEN);
        toShift = makeShift(group, "Ca đổi", LocalDate.now().plusDays(3), ShiftStatus.OPEN);

        approvedReg = new Registration();
        approvedReg.setShift(fromShift);
        approvedReg.setUser(member);
        approvedReg.setPosition(position);
        approvedReg.setStatus(RegistrationStatus.APPROVED);
        approvedReg = registrationRepository.save(approvedReg);
    }

    // ===== B21: createChangeRequest =====

    @Test
    void createChangeRequest_success() {
        CreateChangeRequestDto dto = new CreateChangeRequestDto(
                approvedReg.getId(), toShift.getId(), position.getId(), "Nhờ đổi ca");

        ShiftChangeRequestResponse result = shiftChangeService
                .createChangeRequest(group.getId(), member.getUsername(), dto);

        assertEquals(ShiftChangeRequestStatus.PENDING, result.status());
        assertEquals(approvedReg.getId(), result.fromRegistrationId());
        assertEquals(toShift.getId(), result.toShiftId());
    }

    @Test
    void createChangeRequest_fail_fromRegNotApproved() {
        approvedReg.setStatus(RegistrationStatus.PENDING);
        registrationRepository.save(approvedReg);

        CreateChangeRequestDto dto = new CreateChangeRequestDto(
                approvedReg.getId(), toShift.getId(), position.getId(), null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shiftChangeService.createChangeRequest(group.getId(), member.getUsername(), dto)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createChangeRequest_fail_toShiftNotOpen() {
        Shift lockedShift = makeShift(group, "Ca khóa", LocalDate.now().plusDays(4), ShiftStatus.LOCKED);

        CreateChangeRequestDto dto = new CreateChangeRequestDto(
                approvedReg.getId(), lockedShift.getId(), position.getId(), null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shiftChangeService.createChangeRequest(group.getId(), member.getUsername(), dto)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createChangeRequest_fail_alreadyRegisteredOnToShift() {
        // Member đã có registration trên toShift
        Registration existingReg = new Registration();
        existingReg.setShift(toShift);
        existingReg.setUser(member);
        existingReg.setPosition(position);
        existingReg.setStatus(RegistrationStatus.PENDING);
        registrationRepository.save(existingReg);

        CreateChangeRequestDto dto = new CreateChangeRequestDto(
                approvedReg.getId(), toShift.getId(), position.getId(), null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shiftChangeService.createChangeRequest(group.getId(), member.getUsername(), dto)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ===== B22: approveChangeRequest =====

    @Test
    void approveChangeRequest_success_atomicSwap() {
        CreateChangeRequestDto dto = new CreateChangeRequestDto(
                approvedReg.getId(), toShift.getId(), position.getId(), null);
        ShiftChangeRequestResponse created = shiftChangeService
                .createChangeRequest(group.getId(), member.getUsername(), dto);

        ShiftChangeRequestResponse result = shiftChangeService
                .approveChangeRequest(group.getId(), created.id(), manager.getUsername());

        assertEquals(ShiftChangeRequestStatus.APPROVED, result.status());

        // Registration cũ bị CANCELLED
        Registration oldReg = registrationRepository.findById(approvedReg.getId()).orElseThrow();
        assertEquals(RegistrationStatus.CANCELLED, oldReg.getStatus());

        // Có registration APPROVED mới trên toShift
        assertEquals(1, registrationRepository
                .findByShiftIdAndStatus(toShift.getId(), RegistrationStatus.APPROVED).size());
    }

    @Test
    void approveChangeRequest_fail_notManager() {
        CreateChangeRequestDto dto = new CreateChangeRequestDto(
                approvedReg.getId(), toShift.getId(), position.getId(), null);
        ShiftChangeRequestResponse created = shiftChangeService
                .createChangeRequest(group.getId(), member.getUsername(), dto);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                shiftChangeService.approveChangeRequest(group.getId(), created.id(), member.getUsername())
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // ===== B22: rejectChangeRequest =====

    @Test
    void rejectChangeRequest_success() {
        CreateChangeRequestDto dto = new CreateChangeRequestDto(
                approvedReg.getId(), toShift.getId(), position.getId(), null);
        ShiftChangeRequestResponse created = shiftChangeService
                .createChangeRequest(group.getId(), member.getUsername(), dto);

        ShiftChangeRequestResponse result = shiftChangeService
                .rejectChangeRequest(group.getId(), created.id(), manager.getUsername(), "Không phù hợp");

        assertEquals(ShiftChangeRequestStatus.REJECTED, result.status());
        assertEquals("Không phù hợp", result.managerNote());

        // Registration cũ vẫn APPROVED (không thay đổi)
        Registration oldReg = registrationRepository.findById(approvedReg.getId()).orElseThrow();
        assertEquals(RegistrationStatus.APPROVED, oldReg.getStatus());
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

    private Shift makeShift(Group g, String name, LocalDate date, ShiftStatus status) {
        Shift s = new Shift();
        s.setGroup(g); s.setName(name); s.setDate(date);
        s.setStartTime(LocalTime.of(8, 0)); s.setEndTime(LocalTime.of(12, 0));
        s.setStatus(status);
        return shiftRepository.save(s);
    }
}
