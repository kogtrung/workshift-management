package com.workshift.backend.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.User;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.group.dto.CreateGroupRequest;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class GroupServiceTest {
	@Autowired
	private GroupService groupService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Test
	void createGroup_shouldAssignCreatorAsManager() {
		User creator = new User();
		creator.setUsername("manager_01");
		creator.setEmail("manager_01@example.com");
		creator.setPassword("encoded-password");
		creator.setFullName("Manager 01");
		User savedCreator = userRepository.save(creator);

		var response = groupService.createGroup(
				savedCreator.getUsername(),
				new CreateGroupRequest("Cafe Trung Tâm", "Nhóm ca quán trung tâm")
		);

		assertNotNull(response.id());
		assertNotNull(response.joinCode());
		assertEquals(6, response.joinCode().length());
		assertTrue(groupRepository.findById(response.id()).isPresent());

		var groupMember = groupMemberRepository.findByGroupIdAndUserId(response.id(), savedCreator.getId()).orElseThrow();
		assertEquals(GroupRole.MANAGER, groupMember.getRole());
		assertEquals(GroupMemberStatus.APPROVED, groupMember.getStatus());
		assertNotNull(groupMember.getJoinedAt());
	}

	@Test
	void joinGroup_shouldCreatePendingMember() {
		User manager = new User();
		manager.setUsername("manager_join_01");
		manager.setEmail("manager_join_01@example.com");
		manager.setPassword("encoded-password");
		manager.setFullName("Manager Join 01");
		User savedManager = userRepository.save(manager);

		var createdGroup = groupService.createGroup(
				savedManager.getUsername(),
				new CreateGroupRequest("Cafe Join", "Nhóm để test join")
		);

		User member = new User();
		member.setUsername("member_join_01");
		member.setEmail("member_join_01@example.com");
		member.setPassword("encoded-password");
		member.setFullName("Member Join 01");
		User savedMember = userRepository.save(member);

		var response = groupService.joinGroup(savedMember.getUsername(), createdGroup.id());

		assertEquals(createdGroup.id(), response.groupId());
		assertEquals(savedMember.getId(), response.userId());
		assertEquals(GroupRole.MEMBER.name(), response.role());
		assertEquals(GroupMemberStatus.PENDING.name(), response.status());

		var groupMember = groupMemberRepository.findByGroupIdAndUserId(createdGroup.id(), savedMember.getId()).orElseThrow();
		assertEquals(GroupRole.MEMBER, groupMember.getRole());
		assertEquals(GroupMemberStatus.PENDING, groupMember.getStatus());
		assertNull(groupMember.getJoinedAt());
	}

	@Test
	void joinGroup_shouldRejectDuplicateRequest() {
		User manager = new User();
		manager.setUsername("manager_join_02");
		manager.setEmail("manager_join_02@example.com");
		manager.setPassword("encoded-password");
		manager.setFullName("Manager Join 02");
		User savedManager = userRepository.save(manager);

		var createdGroup = groupService.createGroup(
				savedManager.getUsername(),
				new CreateGroupRequest("Cafe Join Dup", "Nhóm để test duplicate")
		);

		User member = new User();
		member.setUsername("member_join_02");
		member.setEmail("member_join_02@example.com");
		member.setPassword("encoded-password");
		member.setFullName("Member Join 02");
		User savedMember = userRepository.save(member);

		groupService.joinGroup(savedMember.getUsername(), createdGroup.id());

		assertThrows(BusinessException.class, () -> groupService.joinGroup(savedMember.getUsername(), createdGroup.id()));
	}

	@Test
	void joinGroupByCode_shouldCreatePendingMember() {
		User manager = new User();
		manager.setUsername("manager_join_code");
		manager.setEmail("manager_join_code@example.com");
		manager.setPassword("encoded-password");
		manager.setFullName("Manager Join Code");
		User savedManager = userRepository.save(manager);

		var createdGroup = groupService.createGroup(
				savedManager.getUsername(),
				new CreateGroupRequest("Cafe Join Code", "Nhóm để test join code")
		);

		User member = new User();
		member.setUsername("member_join_code");
		member.setEmail("member_join_code@example.com");
		member.setPassword("encoded-password");
		member.setFullName("Member Join Code");
		User savedMember = userRepository.save(member);

		var response = groupService.joinGroupByCode(savedMember.getUsername(), createdGroup.joinCode());

		assertEquals(createdGroup.id(), response.groupId());
		assertEquals(savedMember.getId(), response.userId());
		assertEquals(GroupRole.MEMBER.name(), response.role());
		assertEquals(GroupMemberStatus.PENDING.name(), response.status());
	}
}
