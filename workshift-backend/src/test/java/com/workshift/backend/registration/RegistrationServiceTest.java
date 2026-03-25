package com.workshift.backend.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftRequirement;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.registration.dto.ApproveRegistrationRequest;
import com.workshift.backend.registration.dto.RegisterShiftRequest;
import com.workshift.backend.registration.dto.RegistrationResponse;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.ShiftRequirementRepository;
import com.workshift.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationServiceTest {

	@Autowired
	private RegistrationService registrationService;

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

	private User manager;
	private User member;
	private Group group;
	private Shift shift;
	private Position position;

	@BeforeEach
	void setUp() {
		manager = new User();
		manager.setUsername("manager_test");
		manager.setEmail("manager@test.com");
		manager.setPassword("password");
		manager.setFullName("Manager Test");
		manager = userRepository.save(manager);

		member = new User();
		member.setUsername("member_test");
		member.setEmail("member@test.com");
		member.setPassword("password");
		member.setFullName("Member Test");
		member = userRepository.save(member);

		group = new Group();
		group.setName("Test Group");
		group.setCreatedBy(manager);
		group = groupRepository.save(group);

		// Manager as MANAGER role
		GroupMember managerMember = new GroupMember();
		managerMember.setGroup(group);
		managerMember.setUser(manager);
		managerMember.setRole(GroupRole.MANAGER);
		managerMember.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(managerMember);

		// Member as MEMBER role
		GroupMember groupMember = new GroupMember();
		groupMember.setGroup(group);
		groupMember.setUser(member);
		groupMember.setRole(GroupRole.MEMBER);
		groupMember.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(groupMember);

		position = new Position();
		position.setGroup(group);
		position.setName("Pha chế");
		position = positionRepository.save(position);

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

	private ShiftRequirement addShiftRequirement(int quantity) {
		ShiftRequirement req = new ShiftRequirement();
		req.setShift(shift);
		req.setPosition(position);
		req.setQuantity(quantity);
		return shiftRequirementRepository.save(req);
	}

	private RegistrationResponse registerAsMember() {
		RegisterShiftRequest request = new RegisterShiftRequest();
		request.setPositionId(position.getId());
		request.setNote("Đăng ký ca sáng");
		return registrationService.registerShift(shift.getId(), member.getUsername(), request);
	}

	// ===== Existing tests: registerShift =====

	@Test
	void registerShift_success() {
		RegisterShiftRequest request = new RegisterShiftRequest();
		request.setPositionId(position.getId());
		request.setNote("Đăng ký ca sáng");

		RegistrationResponse response = registrationService.registerShift(shift.getId(), member.getUsername(), request);

		assertNotNull(response.getId());
		assertEquals(shift.getId(), response.getShiftId());
		assertEquals(member.getId(), response.getUserId());
		assertEquals(position.getId(), response.getPositionId());
		assertEquals(RegistrationStatus.PENDING, response.getStatus());
		assertEquals("Đăng ký ca sáng", response.getNote());
	}

	@Test
	void registerShift_fail_userNotFound() {
		RegisterShiftRequest request = new RegisterShiftRequest();
		request.setPositionId(position.getId());

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.registerShift(shift.getId(), "unknown_user", request);
		});

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
		assertEquals("Không tìm thấy người dùng", exception.getMessage());
	}

	@Test
	void registerShift_fail_shiftNotFound() {
		RegisterShiftRequest request = new RegisterShiftRequest();
		request.setPositionId(position.getId());

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.registerShift(999L, member.getUsername(), request);
		});

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
		assertEquals("Không tìm thấy ca làm việc", exception.getMessage());
	}

	@Test
	void registerShift_fail_shiftLocked() {
		shift.setStatus(ShiftStatus.LOCKED);
		shiftRepository.save(shift);

		RegisterShiftRequest request = new RegisterShiftRequest();
		request.setPositionId(position.getId());

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.registerShift(shift.getId(), member.getUsername(), request);
		});

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Không thể đăng ký ca đã khóa hoặc hoàn thành", exception.getMessage());
	}

	@Test
	void registerShift_fail_positionNotFound() {
		RegisterShiftRequest request = new RegisterShiftRequest();
		request.setPositionId(999L);

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.registerShift(shift.getId(), member.getUsername(), request);
		});

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
		assertEquals("Không tìm thấy vị trí", exception.getMessage());
	}

	@Test
	void registerShift_fail_positionNotInGroup() {
		Group otherGroup = new Group();
		otherGroup.setName("Other Group");
		otherGroup.setCreatedBy(manager);
		otherGroup = groupRepository.save(otherGroup);

		Position otherPosition = new Position();
		otherPosition.setGroup(otherGroup);
		otherPosition.setName("Bảo vệ");
		otherPosition = positionRepository.save(otherPosition);

		RegisterShiftRequest request = new RegisterShiftRequest();
		request.setPositionId(otherPosition.getId());

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.registerShift(shift.getId(), member.getUsername(), request);
		});

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Vị trí không thuộc nhóm của ca làm việc", exception.getMessage());
	}

	@Test
	void registerShift_fail_alreadyRegistered() {
		RegisterShiftRequest request1 = new RegisterShiftRequest();
		request1.setPositionId(position.getId());
		registrationService.registerShift(shift.getId(), member.getUsername(), request1);

		RegisterShiftRequest request2 = new RegisterShiftRequest();
		request2.setPositionId(position.getId());

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.registerShift(shift.getId(), member.getUsername(), request2);
		});

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Bạn đã đăng ký ca này rồi", exception.getMessage());
	}

	// ===== Existing tests: cancelRegistration =====

	@Test
	void cancelRegistration_success() {
		RegisterShiftRequest registerReq = new RegisterShiftRequest();
		registerReq.setPositionId(position.getId());
		RegistrationResponse preReg = registrationService.registerShift(shift.getId(), member.getUsername(), registerReq);

		com.workshift.backend.registration.dto.CancelRegistrationRequest cancelReq = new com.workshift.backend.registration.dto.CancelRegistrationRequest();
		cancelReq.setReason("Bận việc đột xuất");

		RegistrationResponse response = registrationService.cancelRegistration(preReg.getId(), member.getUsername(), cancelReq);

		assertNotNull(response);
		assertEquals(RegistrationStatus.CANCELLED, response.getStatus());
		assertEquals("Bận việc đột xuất", response.getNote());
	}

	@Test
	void cancelRegistration_fail_userNotFound() {
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.cancelRegistration(1L, "unknown_user", new com.workshift.backend.registration.dto.CancelRegistrationRequest());
		});
		assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
		assertEquals("Không tìm thấy người dùng", exception.getMessage());
	}

	@Test
	void cancelRegistration_fail_registrationNotFoundOrNotYours() {
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.cancelRegistration(999L, member.getUsername(), new com.workshift.backend.registration.dto.CancelRegistrationRequest());
		});
		assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
		assertEquals("Không tìm thấy đăng ký của bạn", exception.getMessage());
	}

	@Test
	void cancelRegistration_fail_shiftLocked() {
		RegisterShiftRequest registerReq = new RegisterShiftRequest();
		registerReq.setPositionId(position.getId());
		RegistrationResponse preReg = registrationService.registerShift(shift.getId(), member.getUsername(), registerReq);

		shift.setStatus(ShiftStatus.LOCKED);
		shiftRepository.save(shift);

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.cancelRegistration(preReg.getId(), member.getUsername(), new com.workshift.backend.registration.dto.CancelRegistrationRequest());
		});
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Không thể hủy khi ca làm việc đã khóa hoặc hoàn thành", exception.getMessage());
	}

	@Test
	void cancelRegistration_fail_shiftStarted() {
		RegisterShiftRequest registerReq = new RegisterShiftRequest();
		registerReq.setPositionId(position.getId());
		RegistrationResponse preReg = registrationService.registerShift(shift.getId(), member.getUsername(), registerReq);

		shift.setDate(LocalDate.now().minusDays(1));
		shiftRepository.save(shift);

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.cancelRegistration(preReg.getId(), member.getUsername(), new com.workshift.backend.registration.dto.CancelRegistrationRequest());
		});
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Đã quá hạn hủy ca làm việc", exception.getMessage());
	}

	// ===== B14: approveRegistration tests =====

	@Test
	void approveRegistration_success() {
		addShiftRequirement(2);
		RegistrationResponse reg = registerAsMember();

		ApproveRegistrationRequest approveReq = new ApproveRegistrationRequest();
		approveReq.setManagerNote("Đã duyệt");

		RegistrationResponse response = registrationService.approveRegistration(reg.getId(), manager.getUsername(), approveReq);

		assertNotNull(response);
		assertEquals(RegistrationStatus.APPROVED, response.getStatus());
	}

	@Test
	void approveRegistration_fail_notManager() {
		addShiftRequirement(2);
		RegistrationResponse reg = registerAsMember();

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.approveRegistration(reg.getId(), member.getUsername(), null);
		});

		assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
		assertEquals("Chỉ Quản lý mới có quyền duyệt đăng ký ca", exception.getMessage());
	}

	@Test
	void approveRegistration_fail_quotaFull() {
		addShiftRequirement(1); // only 1 slot

		// Member 1 registers and gets approved
		RegistrationResponse reg1 = registerAsMember();
		registrationService.approveRegistration(reg1.getId(), manager.getUsername(), null);

		// Member 2 registers
		User member2 = new User();
		member2.setUsername("member2_test");
		member2.setEmail("member2@test.com");
		member2.setPassword("password");
		member2.setFullName("Member 2");
		member2 = userRepository.save(member2);

		GroupMember gm2 = new GroupMember();
		gm2.setGroup(group);
		gm2.setUser(member2);
		gm2.setRole(GroupRole.MEMBER);
		gm2.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(gm2);

		RegisterShiftRequest req2 = new RegisterShiftRequest();
		req2.setPositionId(position.getId());
		RegistrationResponse reg2 = registrationService.registerShift(shift.getId(), member2.getUsername(), req2);

		// Approve member 2 → should fail, quota full
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.approveRegistration(reg2.getId(), manager.getUsername(), null);
		});

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}

	@Test
	void approveRegistration_fail_shiftLocked() {
		addShiftRequirement(2);
		RegistrationResponse reg = registerAsMember();

		shift.setStatus(ShiftStatus.LOCKED);
		shiftRepository.save(shift);

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.approveRegistration(reg.getId(), manager.getUsername(), null);
		});

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Không thể duyệt đăng ký cho ca đã khóa hoặc hoàn thành", exception.getMessage());
	}

	@Test
	void assignShift_success() {
		com.workshift.backend.registration.dto.AssignShiftRequest assignReq = new com.workshift.backend.registration.dto.AssignShiftRequest();
		assignReq.setUserId(member.getId());
		assignReq.setPositionId(position.getId());
		assignReq.setNote("Force assign test");

		RegistrationResponse response = registrationService.assignShift(shift.getId(), manager.getUsername(), assignReq);

		assertNotNull(response);
		assertEquals(RegistrationStatus.APPROVED, response.getStatus());
		assertEquals("Force assign test", response.getManagerNote());
	}

	@Test
	void assignShift_fail_notManager() {
		com.workshift.backend.registration.dto.AssignShiftRequest assignReq = new com.workshift.backend.registration.dto.AssignShiftRequest();
		assignReq.setUserId(member.getId());
		assignReq.setPositionId(position.getId());

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.assignShift(shift.getId(), member.getUsername(), assignReq);
		});
		assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
		assertEquals("Thao tác yêu cầu quyền Quản lý (MANAGER)", exception.getMessage());
	}
	
	@Test
	void assignShift_fail_shiftLocked() {
		shift.setStatus(ShiftStatus.LOCKED);
		shiftRepository.save(shift);

		com.workshift.backend.registration.dto.AssignShiftRequest assignReq = new com.workshift.backend.registration.dto.AssignShiftRequest();
		assignReq.setUserId(member.getId());
		assignReq.setPositionId(position.getId());

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.assignShift(shift.getId(), manager.getUsername(), assignReq);
		});
		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Không thể gán nhân viên vào ca đã khóa hoặc hoàn thành", exception.getMessage());
	}

	// ===== B15: rejectRegistration tests =====

	@Test
	void rejectRegistration_success() {
		RegistrationResponse reg = registerAsMember();

		com.workshift.backend.registration.dto.RejectRegistrationRequest rejectReq = new com.workshift.backend.registration.dto.RejectRegistrationRequest();
		rejectReq.setReason("Không phù hợp vị trí");

		RegistrationResponse response = registrationService.rejectRegistration(reg.getId(), manager.getUsername(), rejectReq);

		assertNotNull(response);
		assertEquals(RegistrationStatus.REJECTED, response.getStatus());
		assertEquals("Không phù hợp vị trí", response.getManagerNote());
	}

	@Test
	void rejectRegistration_fail_notManager() {
		RegistrationResponse reg = registerAsMember();

		com.workshift.backend.registration.dto.RejectRegistrationRequest rejectReq = new com.workshift.backend.registration.dto.RejectRegistrationRequest();
		rejectReq.setReason("Từ chối");

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.rejectRegistration(reg.getId(), member.getUsername(), rejectReq);
		});

		assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
		assertEquals("Chỉ Quản lý mới có quyền từ chối đăng ký ca", exception.getMessage());
	}

	@Test
	void rejectRegistration_fail_notPending() {
		addShiftRequirement(2);
		RegistrationResponse reg = registerAsMember();

		// Approve first
		ApproveRegistrationRequest approveReq = new ApproveRegistrationRequest();
		approveReq.setManagerNote("Đã duyệt");
		registrationService.approveRegistration(reg.getId(), manager.getUsername(), approveReq);

		com.workshift.backend.registration.dto.RejectRegistrationRequest rejectReq = new com.workshift.backend.registration.dto.RejectRegistrationRequest();
		rejectReq.setReason("Từ chối lại");

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			registrationService.rejectRegistration(reg.getId(), manager.getUsername(), rejectReq);
		});

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Chỉ có thể từ chối đăng ký ở trạng thái chờ duyệt", exception.getMessage());
	}
}
