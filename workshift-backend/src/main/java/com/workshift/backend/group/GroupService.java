package com.workshift.backend.group;

import java.time.Instant;

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
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class GroupService {
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
				savedGroup.getStatus().name(),
				creator.getId()
		);
	}
}
