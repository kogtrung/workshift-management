package com.workshift.backend.group;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.GroupStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.group.dto.CreateGroupRequest;
import com.workshift.backend.group.dto.CreateGroupResponse;
import com.workshift.backend.group.dto.JoinGroupResponse;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class GroupService {
	private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final UserRepository userRepository;

	public GroupService(
			GroupRepository groupRepository,
			GroupMemberRepository groupMemberRepository,
			UserRepository userRepository
	) {
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.userRepository = userRepository;
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

		return new CreateGroupResponse(
				savedGroup.getId(),
				savedGroup.getName(),
				savedGroup.getDescription(),
				savedGroup.getJoinCode(),
				savedGroup.getStatus().name(),
				creator.getId()
		);
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

		return new JoinGroupResponse(
				group.getId(),
				user.getId(),
				groupMember.getRole().name(),
				groupMember.getStatus().name()
		);
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
