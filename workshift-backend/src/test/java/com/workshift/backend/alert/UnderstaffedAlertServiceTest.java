package com.workshift.backend.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.alert.dto.UnderstaffedAlertResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftRequirement;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.ShiftRequirementRepository;
import com.workshift.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnderstaffedAlertServiceTest {

	@Autowired
	private UnderstaffedAlertService understaffedAlertService;

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
	private ShiftRequirementRepository shiftRequirementRepository;

	@Autowired
	private RegistrationRepository registrationRepository;

	private User manager;
	private User member;
	private Group group;
	private Position position1;
	private Position position2;
	private Shift shift;

	@BeforeEach
	void setUp() {
		manager = new User();
		manager.setUsername("alert_manager");
		manager.setEmail("alert_manager@test.com");
		manager.setPassword("password");
		manager.setFullName("Alert Manager");
		manager = userRepository.save(manager);

		member = new User();
		member.setUsername("alert_member");
		member.setEmail("alert_member@test.com");
		member.setPassword("password");
		member.setFullName("Alert Member");
		member = userRepository.save(member);

		group = new Group();
		group.setName("Alert Test Group");
		group.setCreatedBy(manager);
		group = groupRepository.save(group);

		GroupMember managerMember = new GroupMember();
		managerMember.setGroup(group);
		managerMember.setUser(manager);
		managerMember.setRole(GroupRole.MANAGER);
		managerMember.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(managerMember);

		GroupMember groupMember = new GroupMember();
		groupMember.setGroup(group);
		groupMember.setUser(member);
		groupMember.setRole(GroupRole.MEMBER);
		groupMember.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(groupMember);

		position1 = new Position();
		position1.setGroup(group);
		position1.setName("Pha chế");
		position1 = positionRepository.save(position1);

		position2 = new Position();
		position2.setGroup(group);
		position2.setName("Phục vụ");
		position2 = positionRepository.save(position2);

		shift = new Shift();
		shift.setGroup(group);
		shift.setName("Ca sáng");
		shift.setDate(LocalDate.now().plusDays(1));
		shift.setStartTime(LocalTime.of(8, 0));
		shift.setEndTime(LocalTime.of(12, 0));
		shift.setStatus(ShiftStatus.OPEN);
		shift = shiftRepository.save(shift);
	}

	// ===== Helpers =====

	private ShiftRequirement addRequirement(Shift s, Position p, int qty) {
		ShiftRequirement req = new ShiftRequirement();
		req.setShift(s);
		req.setPosition(p);
		req.setQuantity(qty);
		return shiftRequirementRepository.save(req);
	}

	private Registration addApprovedRegistration(Shift s, User u, Position p) {
		Registration reg = new Registration();
		reg.setShift(s);
		reg.setUser(u);
		reg.setPosition(p);
		reg.setStatus(RegistrationStatus.APPROVED);
		return registrationRepository.save(reg);
	}

	// ===== Tests =====

	@Test
	void getUnderstaffedShifts_success_returnsShortages() {
		// Cần 3 Pha chế, chỉ có 1 APPROVED → thiếu 2
		addRequirement(shift, position1, 3);
		addApprovedRegistration(shift, member, position1);

		List<UnderstaffedAlertResponse> result = understaffedAlertService
				.getUnderstaffedShifts(group.getId(), manager.getUsername());

		assertEquals(1, result.size());

		UnderstaffedAlertResponse alert = result.get(0);
		assertEquals(shift.getId(), alert.getShiftId());
		assertEquals("Ca sáng", alert.getShiftName());
		assertEquals(3, alert.getTotalRequired());
		assertEquals(1, alert.getTotalApproved());

		assertEquals(1, alert.getShortages().size());
		var shortage = alert.getShortages().get(0);
		assertEquals(position1.getId(), shortage.positionId());
		assertEquals("Pha chế", shortage.positionName());
		assertEquals(3, shortage.required());
		assertEquals(1, shortage.approved());
		assertEquals(2, shortage.shortage());
	}

	@Test
	void getUnderstaffedShifts_noShortage_returnsEmpty() {
		// Cần 1 Pha chế, có 1 APPROVED → đủ người
		addRequirement(shift, position1, 1);
		addApprovedRegistration(shift, member, position1);

		List<UnderstaffedAlertResponse> result = understaffedAlertService
				.getUnderstaffedShifts(group.getId(), manager.getUsername());

		assertTrue(result.isEmpty());
	}

	@Test
	void getUnderstaffedShifts_noRequirements_returnsEmpty() {
		// Ca không có requirement → bỏ qua
		List<UnderstaffedAlertResponse> result = understaffedAlertService
				.getUnderstaffedShifts(group.getId(), manager.getUsername());

		assertTrue(result.isEmpty());
	}

	@Test
	void getUnderstaffedShifts_fail_notManager() {
		addRequirement(shift, position1, 3);

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			understaffedAlertService.getUnderstaffedShifts(group.getId(), member.getUsername());
		});

		assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
		assertEquals("Chỉ Quản lý mới có quyền xem cảnh báo thiếu người", exception.getMessage());
	}

	@Test
	void getUnderstaffedShifts_multiplePositions_onlyShortageOnesReturned() {
		// Cần 1 Pha chế (đủ) + 3 Phục vụ (thiếu) → chỉ báo Phục vụ
		addRequirement(shift, position1, 1);
		addRequirement(shift, position2, 3);
		addApprovedRegistration(shift, member, position1);

		List<UnderstaffedAlertResponse> result = understaffedAlertService
				.getUnderstaffedShifts(group.getId(), manager.getUsername());

		assertEquals(1, result.size());

		UnderstaffedAlertResponse alert = result.get(0);
		assertNotNull(alert);
		assertEquals(4, alert.getTotalRequired()); // 1 + 3
		assertEquals(1, alert.getTotalApproved());  // chỉ 1 Pha chế

		// Chỉ Phục vụ bị thiếu
		assertEquals(1, alert.getShortages().size());
		var shortage = alert.getShortages().get(0);
		assertEquals(position2.getId(), shortage.positionId());
		assertEquals("Phục vụ", shortage.positionName());
		assertEquals(3, shortage.required());
		assertEquals(0, shortage.approved());
		assertEquals(3, shortage.shortage());
	}
}
