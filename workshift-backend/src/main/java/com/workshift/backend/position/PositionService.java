package com.workshift.backend.position;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.User;
import com.workshift.backend.position.dto.CreatePositionRequest;
import com.workshift.backend.position.dto.PositionResponse;
import com.workshift.backend.position.dto.UpdatePositionRequest;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class PositionService {

	private final PositionRepository positionRepository;
	private final GroupRepository groupRepository;
	private final UserRepository userRepository;
	private final GroupMemberRepository groupMemberRepository;

	public PositionService(
			PositionRepository positionRepository,
			GroupRepository groupRepository,
			UserRepository userRepository,
			GroupMemberRepository groupMemberRepository
	) {
		this.positionRepository = positionRepository;
		this.groupRepository = groupRepository;
		this.userRepository = userRepository;
		this.groupMemberRepository = groupMemberRepository;
	}

	@Transactional
	public PositionResponse createPosition(String username, Long groupId, CreatePositionRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateManagerPermission(groupId, user.getId());

		Group group = groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy group"));

		String trimmedName = request.name().trim();
		if (positionRepository.existsByGroupIdAndName(groupId, trimmedName)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Tên vị trí đã tồn tại trong group");
		}

		Position position = new Position();
		position.setGroup(group);
		position.setName(trimmedName);
		position.setColorCode(request.colorCode() != null ? request.colorCode().trim() : null);

		Position savedPosition = positionRepository.save(position);

		return new PositionResponse(savedPosition.getId(), groupId, savedPosition.getName(), savedPosition.getColorCode());
	}

	@Transactional(readOnly = true)
	public List<PositionResponse> getPositions(String username, Long groupId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateMemberPermission(groupId, user.getId());

		return positionRepository.findByGroupId(groupId).stream()
				.map(p -> new PositionResponse(p.getId(), groupId, p.getName(), p.getColorCode()))
				.toList();
	}

	@Transactional
	public PositionResponse updatePosition(String username, Long groupId, Long positionId, UpdatePositionRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateManagerPermission(groupId, user.getId());

		Position position = positionRepository.findByIdAndGroupId(positionId, groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí"));

		String trimmedName = request.name().trim();
		if (positionRepository.existsByGroupIdAndNameAndIdNot(groupId, trimmedName, positionId)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Tên vị trí đã tồn tại trong group");
		}

		position.setName(trimmedName);
		position.setColorCode(request.colorCode() != null ? request.colorCode().trim() : null);

		return new PositionResponse(position.getId(), groupId, position.getName(), position.getColorCode());
	}

	@Transactional
	public void deletePosition(String username, Long groupId, Long positionId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateManagerPermission(groupId, user.getId());

		Position position = positionRepository.findByIdAndGroupId(positionId, groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí"));

		positionRepository.delete(position);
	}

	private void validateManagerPermission(Long groupId, Long userId) {
		GroupMember managerMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc group này"));
		if (managerMembership.getRole() != GroupRole.MANAGER || managerMembership.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý vị trí");
		}
	}

	private void validateMemberPermission(Long groupId, Long userId) {
		GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc group này"));
		if (membership.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn chưa được duyệt vào group này");
		}
	}
}
