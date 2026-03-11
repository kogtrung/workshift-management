package com.workshift.backend.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.User;
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
		assertTrue(groupRepository.findById(response.id()).isPresent());

		var groupMember = groupMemberRepository.findByGroupIdAndUserId(response.id(), savedCreator.getId()).orElseThrow();
		assertEquals(GroupRole.MANAGER, groupMember.getRole());
		assertEquals(GroupMemberStatus.APPROVED, groupMember.getStatus());
		assertNotNull(groupMember.getJoinedAt());
	}
}
