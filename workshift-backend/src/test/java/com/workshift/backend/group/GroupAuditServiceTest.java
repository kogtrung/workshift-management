package com.workshift.backend.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GroupAuditActionType;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.group.dto.CreateGroupRequest;
import com.workshift.backend.group.dto.GroupMemberReviewAction;
import com.workshift.backend.group.dto.ReviewGroupMemberRequest;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class GroupAuditServiceTest {
	@Autowired
	private GroupService groupService;

	@Autowired
	private GroupAuditService groupAuditService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Test
	void getAuditLogs_shouldReturnEventsForManager() {
		User manager = new User();
		manager.setUsername("audit_manager_01");
		manager.setEmail("audit_manager_01@example.com");
		manager.setPassword("encoded-password");
		manager.setFullName("Audit Manager 01");
		User savedManager = userRepository.save(manager);

		var createdGroup = groupService.createGroup(
				savedManager.getUsername(),
				new CreateGroupRequest("Audit Cafe", "Group để test audit")
		);

		User member = new User();
		member.setUsername("audit_member_01");
		member.setEmail("audit_member_01@example.com");
		member.setPassword("encoded-password");
		member.setFullName("Audit Member 01");
		User savedMember = userRepository.save(member);

		groupService.joinGroupByCode(savedMember.getUsername(), createdGroup.joinCode());
		var pendingMember = groupMemberRepository.findByGroupIdAndUserId(createdGroup.id(), savedMember.getId()).orElseThrow();
		assertEquals(GroupMemberStatus.PENDING, pendingMember.getStatus());

		groupService.reviewMember(
				savedManager.getUsername(),
				createdGroup.id(),
				pendingMember.getId(),
				new ReviewGroupMemberRequest(GroupMemberReviewAction.APPROVE)
		);

		var pageData = groupAuditService.getAuditLogs(
				savedManager.getUsername(),
				createdGroup.id(),
				LocalDate.now().minusDays(1),
				LocalDate.now().plusDays(1),
				null,
				null,
				null,
				null,
				0,
				20
		);

		assertFalse(pageData.items().isEmpty());
		assertEquals(3, pageData.items().size());
	}

	@Test
	void getAuditLogs_shouldDenyMemberRole() {
		User manager = new User();
		manager.setUsername("audit_manager_02");
		manager.setEmail("audit_manager_02@example.com");
		manager.setPassword("encoded-password");
		manager.setFullName("Audit Manager 02");
		User savedManager = userRepository.save(manager);

		var createdGroup = groupService.createGroup(
				savedManager.getUsername(),
				new CreateGroupRequest("Audit Cafe 2", "Group để test quyền audit")
		);

		User member = new User();
		member.setUsername("audit_member_02");
		member.setEmail("audit_member_02@example.com");
		member.setPassword("encoded-password");
		member.setFullName("Audit Member 02");
		User savedMember = userRepository.save(member);

		groupService.joinGroupByCode(savedMember.getUsername(), createdGroup.joinCode());

		assertThrows(BusinessException.class, () -> groupAuditService.getAuditLogs(
				savedMember.getUsername(),
				createdGroup.id(),
				null,
				null,
				GroupAuditActionType.GROUP_MEMBER_JOIN_REQUESTED,
				null,
				null,
				null,
				0,
				20
		));
	}
}
