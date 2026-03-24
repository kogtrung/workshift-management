package com.workshift.backend.shiftrequirement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftRequirement;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.ShiftRequirementRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shiftrequirement.dto.ShiftRequirementResponse;
import com.workshift.backend.shiftrequirement.dto.UpsertShiftRequirementRequest;

@Service
public class ShiftRequirementService {

	private final ShiftRequirementRepository shiftRequirementRepository;
	private final ShiftRepository shiftRepository;
	private final PositionRepository positionRepository;
	private final UserRepository userRepository;
	private final GroupMemberRepository groupMemberRepository;

	public ShiftRequirementService(
			ShiftRequirementRepository shiftRequirementRepository,
			ShiftRepository shiftRepository,
			PositionRepository positionRepository,
			UserRepository userRepository,
			GroupMemberRepository groupMemberRepository
	) {
		this.shiftRequirementRepository = shiftRequirementRepository;
		this.shiftRepository = shiftRepository;
		this.positionRepository = positionRepository;
		this.userRepository = userRepository;
		this.groupMemberRepository = groupMemberRepository;
	}

	@Transactional
	public ShiftRequirementResponse createRequirement(
			String username,
			Long shiftId,
			UpsertShiftRequirementRequest request
	) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		Shift shift = shiftRepository.findById(shiftId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

		Long groupId = shift.getGroup().getId();
		validateManagerPermission(groupId, user.getId());

		Position position = positionRepository.findByIdAndGroupId(request.positionId(), groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí"));

		if (shiftRequirementRepository.findByShiftIdAndPositionId(shiftId, request.positionId()).isPresent()) {
			throw new BusinessException(HttpStatus.CONFLICT, "Vị trí đã được cấu hình trong ca này");
		}

		ShiftRequirement requirement = new ShiftRequirement();
		requirement.setShift(shift);
		requirement.setPosition(position);

		requirement.setQuantity(request.quantity());

		ShiftRequirement saved = shiftRequirementRepository.save(requirement);
		return toResponse(saved);
	}

	@Transactional
	public ShiftRequirementResponse updateRequirement(
			String username,
			Long shiftId,
			Long requirementId,
			UpsertShiftRequirementRequest request
	) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		Shift shift = shiftRepository.findById(shiftId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

		Long groupId = shift.getGroup().getId();
		validateManagerPermission(groupId, user.getId());

		ShiftRequirement requirement = shiftRequirementRepository.findByIdAndShiftId(requirementId, shiftId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình nhu cầu"));

		Position position = positionRepository.findByIdAndGroupId(request.positionId(), groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí"));

		if (!requirement.getPosition().getId().equals(request.positionId())
				&& shiftRequirementRepository.findByShiftIdAndPositionId(shiftId, request.positionId()).isPresent()) {
			throw new BusinessException(HttpStatus.CONFLICT, "Vị trí đã được cấu hình trong ca này");
		}

		requirement.setPosition(position);
		requirement.setQuantity(request.quantity());

		return toResponse(requirement);
	}

	@Transactional(readOnly = true)
	public List<ShiftRequirementResponse> getRequirements(String username, Long shiftId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		Shift shift = shiftRepository.findById(shiftId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

		validateMemberPermission(shift.getGroup().getId(), user.getId());

		return shiftRequirementRepository.findByShiftId(shiftId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public void deleteRequirement(String username, Long shiftId, Long requirementId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		Shift shift = shiftRepository.findById(shiftId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

		validateManagerPermission(shift.getGroup().getId(), user.getId());

		ShiftRequirement requirement = shiftRequirementRepository.findByIdAndShiftId(requirementId, shiftId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình nhu cầu"));

		shiftRequirementRepository.delete(requirement);
	}

	private void validateManagerPermission(Long groupId, Long userId) {
		GroupMember managerMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc group này"));
		if (managerMembership.getRole() != GroupRole.MANAGER || managerMembership.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền cấu hình nhu cầu");
		}
	}

	private void validateMemberPermission(Long groupId, Long userId) {
		GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc group này"));
		if (membership.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn chưa được duyệt vào group này");
		}
	}

	private ShiftRequirementResponse toResponse(ShiftRequirement requirement) {
		return new ShiftRequirementResponse(
				requirement.getId(),
				requirement.getShift().getId(),
				requirement.getPosition().getId(),
				requirement.getPosition().getName(),
				requirement.getQuantity()
		);
	}
}

