package com.workshift.backend.shiftrequirement;

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
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.ShiftRequirementRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shiftrequirement.dto.UpsertShiftRequirementRequest;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShiftRequirementServiceTest {

	@Autowired
	private ShiftRequirementService shiftRequirementService;

	@Autowired
	private ShiftRequirementRepository shiftRequirementRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private PositionRepository positionRepository;

	@Autowired
	private ShiftRepository shiftRepository;

	private User manager;
	private User member;
	private Group group;
	private Position position;
	private Shift shift;

	@BeforeEach
	void setUp() {
		manager = new User();
		manager.setUsername("manager_req");
		manager.setEmail("manager_req@example.com");
		manager.setPassword("encoded-password");
		manager.setFullName("Manager Req");
		manager = userRepository.save(manager);

		member = new User();
		member.setUsername("member_req");
		member.setEmail("member_req@example.com");
		member.setPassword("encoded-password");
		member.setFullName("Member Req");
		member = userRepository.save(member);

		group = new Group();
		group.setName("Group Req");
		group.setCreatedBy(manager);
		group = groupRepository.save(group);

		GroupMember managerMembership = new GroupMember();
		managerMembership.setGroup(group);
		managerMembership.setUser(manager);
		managerMembership.setRole(GroupRole.MANAGER);
		managerMembership.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(managerMembership);

		GroupMember memberMembership = new GroupMember();
		memberMembership.setGroup(group);
		memberMembership.setUser(member);
		memberMembership.setRole(GroupRole.MEMBER);
		memberMembership.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(memberMembership);

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

	@Test
	void requirement_shouldCreateAndUpdate() {
		var created = shiftRequirementService.createRequirement(
				manager.getUsername(),
				shift.getId(),
				new UpsertShiftRequirementRequest(position.getId(), 2)
		);

		assertNotNull(created.id());
		assertEquals(2, created.quantity());

		var updated = shiftRequirementService.updateRequirement(
				manager.getUsername(),
				shift.getId(),
				created.id(),
				new UpsertShiftRequirementRequest(position.getId(), 3)
		);

		assertEquals(created.id(), updated.id());
		assertEquals(3, updated.quantity());
		assertEquals(1, shiftRequirementRepository.findByShiftId(shift.getId()).size());
	}

	@Test
	void createRequirement_shouldRejectMember() {
		BusinessException exception = assertThrows(BusinessException.class, () -> shiftRequirementService.createRequirement(
				member.getUsername(),
				shift.getId(),
				new UpsertShiftRequirementRequest(position.getId(), 2)
		));

		assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
	}

	@Test
	void createRequirement_shouldRejectDuplicatePosition() {
		shiftRequirementService.createRequirement(
				manager.getUsername(),
				shift.getId(),
				new UpsertShiftRequirementRequest(position.getId(), 2)
		);

		BusinessException exception = assertThrows(BusinessException.class, () -> shiftRequirementService.createRequirement(
				manager.getUsername(),
				shift.getId(),
				new UpsertShiftRequirementRequest(position.getId(), 3)
		));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}
}

