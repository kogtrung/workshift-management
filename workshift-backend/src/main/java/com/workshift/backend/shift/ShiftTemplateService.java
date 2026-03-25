package com.workshift.backend.shift;

import java.util.ArrayList;
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
import com.workshift.backend.domain.ShiftTemplate;
import com.workshift.backend.domain.TemplateRequirement;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftTemplateRepository;
import com.workshift.backend.repository.TemplateRequirementRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shift.dto.template.CreateShiftTemplateRequest;
import com.workshift.backend.shift.dto.template.ShiftTemplateResponse;
import com.workshift.backend.shift.dto.template.TemplateRequirementItem;
import com.workshift.backend.shift.dto.template.TemplateRequirementResponse;
import com.workshift.backend.shift.dto.template.UpdateShiftTemplateRequest;

@Service
public class ShiftTemplateService {

	private final ShiftTemplateRepository shiftTemplateRepository;
	private final GroupRepository groupRepository;
	private final UserRepository userRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final TemplateRequirementRepository templateRequirementRepository;
	private final PositionRepository positionRepository;

	public ShiftTemplateService(
			ShiftTemplateRepository shiftTemplateRepository,
			GroupRepository groupRepository,
			UserRepository userRepository,
			GroupMemberRepository groupMemberRepository,
			TemplateRequirementRepository templateRequirementRepository,
			PositionRepository positionRepository
	) {
		this.shiftTemplateRepository = shiftTemplateRepository;
		this.groupRepository = groupRepository;
		this.userRepository = userRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.templateRequirementRepository = templateRequirementRepository;
		this.positionRepository = positionRepository;
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

		// Save template requirements if provided
		List<TemplateRequirement> savedReqs = new ArrayList<>();
		if (request.requirements() != null && !request.requirements().isEmpty()) {
			for (TemplateRequirementItem item : request.requirements()) {
				Position pos = positionRepository.findByIdAndGroupId(item.positionId(), groupId)
						.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí: " + item.positionId()));
				TemplateRequirement tr = new TemplateRequirement();
				tr.setTemplate(saved);
				tr.setPosition(pos);
				tr.setQuantity(item.quantity());
				savedReqs.add(templateRequirementRepository.save(tr));
			}
		}

		return toResponse(saved, savedReqs);
	}

	@Transactional(readOnly = true)
	public List<ShiftTemplateResponse> getTemplates(String username, Long groupId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateMemberPermission(groupId, user.getId());

		List<ShiftTemplate> templates = shiftTemplateRepository.findByGroupId(groupId);
		return templates.stream()
				.map(t -> toResponse(t, templateRequirementRepository.findByTemplateId(t.getId())))
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

		// Update requirements: delete old, save new
		templateRequirementRepository.deleteByTemplateId(templateId);
		templateRequirementRepository.flush();
		List<TemplateRequirement> savedReqs = new ArrayList<>();
		if (request.requirements() != null && !request.requirements().isEmpty()) {
			for (TemplateRequirementItem item : request.requirements()) {
				Position pos = positionRepository.findByIdAndGroupId(item.positionId(), groupId)
						.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí: " + item.positionId()));
				TemplateRequirement tr = new TemplateRequirement();
				tr.setTemplate(template);
				tr.setPosition(pos);
				tr.setQuantity(item.quantity());
				savedReqs.add(templateRequirementRepository.save(tr));
			}
		}

		return toResponse(template, savedReqs);
	}

	@Transactional
	public void deleteTemplate(String username, Long groupId, Long templateId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		validateManagerPermission(groupId, user.getId());

		ShiftTemplate template = shiftTemplateRepository.findByIdAndGroupId(templateId, groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca mẫu"));

		templateRequirementRepository.deleteByTemplateId(templateId);
		shiftTemplateRepository.delete(template);
	}

	private ShiftTemplateResponse toResponse(ShiftTemplate template, List<TemplateRequirement> reqs) {
		List<TemplateRequirementResponse> reqResponses = reqs == null ? List.of() : reqs.stream()
				.map(r -> new TemplateRequirementResponse(
						r.getId(),
						r.getPosition().getId(),
						r.getPosition().getName(),
						r.getPosition().getColorCode(),
						r.getQuantity()
				))
				.toList();
		return new ShiftTemplateResponse(
				template.getId(),
				template.getGroup().getId(),
				template.getName(),
				template.getStartTime(),
				template.getEndTime(),
				template.getDescription(),
				reqResponses
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
