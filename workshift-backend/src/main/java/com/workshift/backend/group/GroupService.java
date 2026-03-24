package com.workshift.backend.group;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupAuditActionType;
import com.workshift.backend.domain.GroupAuditActorRole;
import com.workshift.backend.domain.GroupAuditEntityType;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.GroupStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.group.dto.MyGroupResponse;
import com.workshift.backend.group.dto.GroupMemberDetailResponse;
import com.workshift.backend.group.dto.CreateGroupRequest;
import com.workshift.backend.group.dto.CreateGroupResponse;
import com.workshift.backend.group.dto.GroupMemberResponse;
import com.workshift.backend.group.dto.JoinGroupResponse;
import com.workshift.backend.group.dto.ReviewGroupMemberRequest;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class GroupService {
	private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final UserRepository userRepository;
	private final GroupAuditService groupAuditService;

	public GroupService(
			GroupRepository groupRepository,
			GroupMemberRepository groupMemberRepository,
			UserRepository userRepository,
			GroupAuditService groupAuditService
	) {
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.userRepository = userRepository;
		this.groupAuditService = groupAuditService;
	}

	@Transactional
	public CreateGroupResponse createGroup(String username, CreateGroupRequest request) {
		User creator = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		Group group = new Group();
		group.setName(request.name().trim());
		group.setDescription(request.description());
		group.setJoinCode(generateUniqueJoinCode());
		group.setCreatedBy(creator);
		group.setStatus(GroupStatus.ACTIVE);

		Group savedGroup = groupRepository.save(group);

		GroupMember managerMember = new GroupMember();
		managerMember.setGroup(savedGroup);
		managerMember.setUser(creator);
		managerMember.setRole(GroupRole.MANAGER);
		managerMember.setStatus(GroupMemberStatus.APPROVED);
		managerMember.setJoinedAt(Instant.now());
		groupMemberRepository.save(managerMember);

		groupAuditService.recordEvent(
				savedGroup,
				creator,
				GroupAuditActorRole.MANAGER,
				GroupAuditActionType.GROUP_CREATED,
				GroupAuditEntityType.GROUP,
				savedGroup.getId(),
				"Tạo group mới",
				null,
				Map.of(
						"groupId", savedGroup.getId(),
						"name", savedGroup.getName(),
						"joinCode", savedGroup.getJoinCode()
				),
				Map.of("source", "create_group")
		);

		return new CreateGroupResponse(
				savedGroup.getId(),
				savedGroup.getName(),
				savedGroup.getDescription(),
				savedGroup.getJoinCode(),
				savedGroup.getStatus().name(),
				creator.getId()
		);
	}

	@Transactional(readOnly = true)
	public List<MyGroupResponse> getMyGroups(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		List<GroupMemberStatus> activeStatuses = List.of(GroupMemberStatus.APPROVED, GroupMemberStatus.PENDING);

		return groupMemberRepository.findAllByUserIdAndStatusIn(user.getId(), activeStatuses)
				.stream()
				.map(member -> {
					Group group = member.getGroup();
					return new MyGroupResponse(
							group.getId(),
							group.getName(),
							group.getDescription(),
							group.getJoinCode(),
							group.getStatus().name(),
							member.getRole().name(),
							member.getStatus().name()
					);
				})
				.toList();
	}

	@Transactional
	public JoinGroupResponse joinGroup(String username, Long groupId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		Group group = groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy group"));
		return joinGroupInternal(user, group);
	}

	@Transactional
	public JoinGroupResponse joinGroupByCode(String username, String rawJoinCode) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		String normalizedJoinCode = rawJoinCode.trim().toUpperCase(Locale.ROOT);
		Group group = groupRepository.findByJoinCode(normalizedJoinCode)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Mã tham gia không hợp lệ"));
		return joinGroupInternal(user, group);
	}

	@Transactional(readOnly = true)
	public List<GroupMemberDetailResponse> getGroupMembers(String username, Long groupId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		// Any group member (APPROVED) can view the members list
		GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc group này"));
		if (membership.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn chưa được duyệt vào group này");
		}

		return groupMemberRepository.findAllByGroupIdAndStatus(groupId, GroupMemberStatus.APPROVED)
				.stream()
				.map(member -> {
					User memberUser = member.getUser();
					return new GroupMemberDetailResponse(
							member.getId(),
							member.getGroup().getId(),
							memberUser.getId(),
							memberUser.getUsername(),
							memberUser.getFullName(),
							memberUser.getEmail(),
							member.getRole().name(),
							member.getStatus().name(),
							member.getJoinedAt()
					);
				})
				.toList();
	}

	@Transactional
	public void leaveGroup(String username, Long groupId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Bạn không thuộc group này"));

		if (membership.getRole() == GroupRole.MANAGER) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Manager không thể rời group. Hãy chuyển quyền quản lý trước.");
		}

		groupAuditService.recordEvent(
				membership.getGroup(),
				user,
				GroupAuditActorRole.MEMBER,
				GroupAuditActionType.GROUP_MEMBER_REJECTED,
				GroupAuditEntityType.GROUP_MEMBER,
				membership.getId(),
				"Thành viên rời group",
				Map.of("status", membership.getStatus().name()),
				Map.of("status", "LEFT"),
				Map.of("source", "leave_group")
		);

		groupMemberRepository.delete(membership);
	}

	@Transactional(readOnly = true)
	public List<GroupMemberDetailResponse> getPendingMembers(String username, Long groupId) {
		User manager = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));
		validateManagerPermission(groupId, manager.getId());

		return groupMemberRepository.findAllByGroupIdAndStatus(groupId, GroupMemberStatus.PENDING)
				.stream()
				.map(member -> {
					User memberUser = member.getUser();
					return new GroupMemberDetailResponse(
							member.getId(),
							member.getGroup().getId(),
							memberUser.getId(),
							memberUser.getUsername(),
							memberUser.getFullName(),
							memberUser.getEmail(),
							member.getRole().name(),
							member.getStatus().name(),
							member.getJoinedAt()
					);
				})
				.toList();
	}

	@Transactional
	public GroupMemberResponse reviewMember(String username, Long groupId, Long memberId, ReviewGroupMemberRequest request) {
		User manager = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));
		validateManagerPermission(groupId, manager.getId());

		GroupMember groupMember = groupMemberRepository.findByIdAndGroupId(memberId, groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy thành viên trong group"));

		if (groupMember.getStatus() != GroupMemberStatus.PENDING) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Chỉ có thể duyệt yêu cầu ở trạng thái PENDING");
		}

		GroupMemberStatus previousStatus = groupMember.getStatus();
		Instant previousJoinedAt = groupMember.getJoinedAt();
		switch (request.action()) {
			case APPROVE -> {
				groupMember.setStatus(GroupMemberStatus.APPROVED);
				groupMember.setJoinedAt(Instant.now());
			}
			case REJECT -> {
				groupMember.setStatus(GroupMemberStatus.REJECTED);
				groupMember.setJoinedAt(null);
			}
		}

		groupAuditService.recordEvent(
				groupMember.getGroup(),
				manager,
				GroupAuditActorRole.MANAGER,
				request.action().name().equals("APPROVE")
						? GroupAuditActionType.GROUP_MEMBER_APPROVED
						: GroupAuditActionType.GROUP_MEMBER_REJECTED,
				GroupAuditEntityType.GROUP_MEMBER,
				groupMember.getId(),
				"Cập nhật trạng thái thành viên",
				Map.of(
						"status", previousStatus.name(),
						"joinedAt", previousJoinedAt == null ? "" : previousJoinedAt.toString()
				),
				Map.of(
						"status", groupMember.getStatus().name(),
						"joinedAt", groupMember.getJoinedAt() == null ? "" : groupMember.getJoinedAt().toString()
				),
				Map.of(
						"targetUserId", groupMember.getUser().getId(),
						"action", request.action().name()
				)
		);

		return new GroupMemberResponse(
				groupMember.getId(),
				groupMember.getGroup().getId(),
				groupMember.getUser().getId(),
				groupMember.getRole().name(),
				groupMember.getStatus().name()
		);
	}

	private JoinGroupResponse joinGroupInternal(User user, Group group) {
		if (group.getStatus() != GroupStatus.ACTIVE) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Group hiện không hoạt động");
		}

		if (groupMemberRepository.findByGroupIdAndUserId(group.getId(), user.getId()).isPresent()) {
			throw new BusinessException(HttpStatus.CONFLICT, "Bạn đã tham gia hoặc gửi yêu cầu vào group này");
		}

		GroupMember groupMember = new GroupMember();
		groupMember.setGroup(group);
		groupMember.setUser(user);
		groupMember.setRole(GroupRole.MEMBER);
		groupMember.setStatus(GroupMemberStatus.PENDING);
		groupMember.setJoinedAt(null);
		groupMemberRepository.save(groupMember);

		groupAuditService.recordEvent(
				group,
				user,
				GroupAuditActorRole.MEMBER,
				GroupAuditActionType.GROUP_MEMBER_JOIN_REQUESTED,
				GroupAuditEntityType.GROUP_MEMBER,
				groupMember.getId(),
				"Gửi yêu cầu tham gia group",
				null,
				Map.of(
						"groupId", group.getId(),
						"userId", user.getId(),
						"status", groupMember.getStatus().name()
				),
				Map.of("source", "join_group")
		);

		return new JoinGroupResponse(
				group.getId(),
				user.getId(),
				groupMember.getRole().name(),
				groupMember.getStatus().name()
		);
	}

	private void validateManagerPermission(Long groupId, Long userId) {
		GroupMember managerMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc group này"));
		if (managerMembership.getRole() != GroupRole.MANAGER || managerMembership.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền duyệt thành viên");
		}
	}

	private String generateUniqueJoinCode() {
		for (int i = 0; i < 20; i++) {
			StringBuilder code = new StringBuilder();
			for (int j = 0; j < 6; j++) {
				int index = ThreadLocalRandom.current().nextInt(JOIN_CODE_ALPHABET.length());
				code.append(JOIN_CODE_ALPHABET.charAt(index));
			}
			String joinCode = code.toString();
			if (!groupRepository.existsByJoinCode(joinCode)) {
				return joinCode;
			}
		}
		throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo mã tham gia group, vui lòng thử lại");
	}
}
