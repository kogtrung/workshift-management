package com.workshift.backend.shift;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.ShiftTemplate;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.ShiftTemplateRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shift.dto.template.CreateShiftTemplateRequest;
import com.workshift.backend.shift.dto.template.ShiftTemplateResponse;
import com.workshift.backend.shift.dto.template.UpdateShiftTemplateRequest;

@Service
public class ShiftTemplateService {

	private final ShiftTemplateRepository shiftTemplateRepository;
	private final GroupRepository groupRepository;
	private final UserRepository userRepository;
	private final GroupMemberRepository groupMemberRepository;

	public ShiftTemplateService(
			ShiftTemplateRepository shiftTemplateRepository,
			GroupRepository groupRepository,
			UserRepository userRepository,
			GroupMemberRepository groupMemberRepository
	) {
		this.shiftTemplateRepository = shiftTemplateRepository;
		this.groupRepository = groupRepository;
		this.userRepository = userRepository;
		this.groupMemberRepository = groupMemberRepository;
	}

	@Transactional
	public ShiftTemplateResponse createTemplate(String username, Long groupId, CreateShiftTemplateRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateManagerPermission(groupId, user.getId());

		Group group = groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy group"));

		if (request.endTime().isBefore(request.startTime()) || request.endTime().equals(request.startTime())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Giờ kết thúc phải sau giờ bắt đầu");
		}

		String trimmedName = request.name().trim();
		if (shiftTemplateRepository.existsByGroupIdAndName(groupId, trimmedName)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Tên ca mẫu đã tồn tại trong group");
		}

		ShiftTemplate template = new ShiftTemplate();
		template.setGroup(group);
		template.setName(trimmedName);
		template.setStartTime(request.startTime());
		template.setEndTime(request.endTime());
		template.setDescription(request.description() != null ? request.description().trim() : null);

		ShiftTemplate saved = shiftTemplateRepository.save(template);

		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<ShiftTemplateResponse> getTemplates(String username, Long groupId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateMemberPermission(groupId, user.getId());

		return shiftTemplateRepository.findByGroupId(groupId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public ShiftTemplateResponse updateTemplate(String username, Long groupId, Long templateId, UpdateShiftTemplateRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateManagerPermission(groupId, user.getId());

		ShiftTemplate template = shiftTemplateRepository.findByIdAndGroupId(templateId, groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca mẫu"));

		if (request.endTime().isBefore(request.startTime()) || request.endTime().equals(request.startTime())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Giờ kết thúc phải sau giờ bắt đầu");
		}

		String trimmedName = request.name().trim();
		if (shiftTemplateRepository.existsByGroupIdAndNameAndIdNot(groupId, trimmedName, templateId)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Tên ca mẫu đã tồn tại trong group");
		}

		template.setName(trimmedName);
		template.setStartTime(request.startTime());
		template.setEndTime(request.endTime());
		template.setDescription(request.description() != null ? request.description().trim() : null);

		return toResponse(template);
	}

	@Transactional
	public void deleteTemplate(String username, Long groupId, Long templateId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateManagerPermission(groupId, user.getId());

		ShiftTemplate template = shiftTemplateRepository.findByIdAndGroupId(templateId, groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca mẫu"));

		shiftTemplateRepository.delete(template);
	}

	private ShiftTemplateResponse toResponse(ShiftTemplate template) {
		return new ShiftTemplateResponse(
				template.getId(),
				template.getGroup().getId(),
				template.getName(),
				template.getStartTime(),
				template.getEndTime(),
				template.getDescription()
		);
	}

	private void validateManagerPermission(Long groupId, Long userId) {
		GroupMember managerMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc group này"));
		if (managerMembership.getRole() != GroupRole.MANAGER || managerMembership.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý ca mẫu");
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
