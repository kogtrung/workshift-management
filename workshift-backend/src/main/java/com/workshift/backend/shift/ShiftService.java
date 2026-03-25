package com.workshift.backend.shift;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Availability;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftRequirement;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.ShiftTemplate;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.AvailabilityRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.ShiftRequirementRepository;
import com.workshift.backend.repository.ShiftTemplateRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shift.dto.AvailableShiftResponse;
import com.workshift.backend.shift.dto.CreateShiftRequest;
import com.workshift.backend.shift.dto.CreateShiftResponse;
import com.workshift.backend.shiftrequirement.dto.ShiftRequirementResponse;
import com.workshift.backend.shift.dto.AssignedMemberResponse;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.TemplateRequirement;
import com.workshift.backend.repository.TemplateRequirementRepository;
import com.workshift.backend.repository.PositionRepository;

@Service
public class ShiftService {

	private final ShiftRepository shiftRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final UserRepository userRepository;
	private final ShiftTemplateRepository shiftTemplateRepository;
	private final AvailabilityRepository availabilityRepository;
	private final ShiftRequirementRepository shiftRequirementRepository;
	private final RegistrationRepository registrationRepository;
	private final TemplateRequirementRepository templateRequirementRepository;
	private final PositionRepository positionRepository;

	public ShiftService(ShiftRepository shiftRepository,
						GroupRepository groupRepository,
						GroupMemberRepository groupMemberRepository,
						UserRepository userRepository,
						ShiftTemplateRepository shiftTemplateRepository,
						AvailabilityRepository availabilityRepository,
						ShiftRequirementRepository shiftRequirementRepository,
						RegistrationRepository registrationRepository,
						TemplateRequirementRepository templateRequirementRepository,
						PositionRepository positionRepository) {
		this.shiftRepository = shiftRepository;
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.userRepository = userRepository;
		this.shiftTemplateRepository = shiftTemplateRepository;
		this.availabilityRepository = availabilityRepository;
		this.shiftRequirementRepository = shiftRequirementRepository;
		this.registrationRepository = registrationRepository;
		this.templateRequirementRepository = templateRequirementRepository;
		this.positionRepository = positionRepository;
	}

	@Transactional(readOnly = true)
	public List<CreateShiftResponse> getShifts(Long groupId, String username, LocalDate from, LocalDate to) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

		groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		List<Shift> shifts;
		if (from != null && to != null) {
			shifts = shiftRepository.findByGroupIdAndDateBetweenOrderByDateAscStartTimeAsc(groupId, from, to);
		} else {
			shifts = shiftRepository.findByGroupIdOrderByDateAscStartTimeAsc(groupId);
		}

		// batch load requirements
		List<Long> shiftIds = shifts.stream().map(Shift::getId).toList();
		Map<Long, List<ShiftRequirement>> reqMap = shiftIds.isEmpty()
				? Map.of()
				: shiftRequirementRepository.findByShiftIdIn(shiftIds).stream()
						.collect(Collectors.groupingBy(r -> r.getShift().getId()));

		// batch load assigned members
		Map<Long, List<Registration>> regMap = shiftIds.isEmpty()
				? Map.of()
				: registrationRepository.findByShiftIdInAndStatus(shiftIds, RegistrationStatus.APPROVED).stream()
						.collect(Collectors.groupingBy(r -> r.getShift().getId()));

		return shifts.stream().map(s -> toResponseWithReqsAndRegs(s, reqMap.getOrDefault(s.getId(), List.of()), regMap.getOrDefault(s.getId(), List.of()))).toList();
	}

	private CreateShiftResponse toResponseWithReqsAndRegs(Shift shift, List<ShiftRequirement> reqs, List<Registration> regs) {
		CreateShiftResponse response = toResponse(shift);
		List<ShiftRequirementResponse> reqResponses = reqs.stream()
				.map(r -> new ShiftRequirementResponse(r.getId(), shift.getId(), r.getPosition().getId(), r.getPosition().getName(), r.getPosition().getColorCode(), r.getQuantity()))
				.toList();
		response.setRequirements(reqResponses);
		response.setTotalRequired(reqs.stream().mapToInt(ShiftRequirement::getQuantity).sum());
		
		List<AssignedMemberResponse> regResponses = regs.stream()
				.map(r -> new AssignedMemberResponse(
						r.getUser().getId(),
						r.getUser().getFullName() != null && !r.getUser().getFullName().isBlank() ? r.getUser().getFullName() : r.getUser().getUsername(),
						r.getUser().getUsername(),
						r.getPosition().getId(),
						r.getPosition().getName(),
						r.getPosition().getColorCode()
				))
				.toList();
		response.setAssignedMembers(regResponses);
		
		return response;
	}

	private CreateShiftResponse toResponse(Shift shift) {
		CreateShiftResponse response = new CreateShiftResponse();
		response.setId(shift.getId());
		response.setGroupId(shift.getGroup().getId());
		response.setTemplateId(shift.getTemplate() != null ? shift.getTemplate().getId() : null);
		response.setName(shift.getName());
		response.setDate(shift.getDate());
		response.setStartTime(shift.getStartTime());
		response.setEndTime(shift.getEndTime());
		response.setNote(shift.getNote());
		response.setStatus(shift.getStatus());
		response.setRequirements(new ArrayList<>());
		response.setTotalRequired(0);
		return response;
	}

	@Transactional
	public CreateShiftResponse createShift(Long groupId, String username, CreateShiftRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		Group group = groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền tạo ca làm việc");
		}

		ResolvedShiftInput resolved = resolveInput(groupId, request);

		List<Shift> overlappingShifts = shiftRepository.findOverlappingShifts(
				groupId, 
				request.getDate(), 
				resolved.startTime(), 
				resolved.endTime()
		);

		if (!overlappingShifts.isEmpty()) {
			throw new BusinessException(HttpStatus.CONFLICT, "Ca làm việc bị trùng lặp thời gian với ca khác trong cùng ngày");
		}

		Shift shift = new Shift();
		shift.setGroup(group);
		shift.setTemplate(resolved.template());
		shift.setName(resolved.name());
		shift.setDate(request.getDate());
		shift.setStartTime(resolved.startTime());
		shift.setEndTime(resolved.endTime());
		shift.setNote(resolved.note());
		shift.setStatus(ShiftStatus.OPEN);

		Shift savedShift = shiftRepository.save(shift);

		// Auto-copy template requirements
		if (resolved.template() != null) {
			copyTemplateRequirements(savedShift, resolved.template().getId());
		}

		CreateShiftResponse response = new CreateShiftResponse();
		response.setId(savedShift.getId());
		response.setGroupId(groupId);
		response.setTemplateId(savedShift.getTemplate() != null ? savedShift.getTemplate().getId() : null);
		response.setName(savedShift.getName());
		response.setDate(savedShift.getDate());
		response.setStartTime(savedShift.getStartTime());
		response.setEndTime(savedShift.getEndTime());
		response.setNote(savedShift.getNote());
		response.setStatus(savedShift.getStatus());

		return response;
	}

	@Transactional
	public List<CreateShiftResponse> createShiftsBulk(Long groupId, String username, List<CreateShiftRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Danh sách ca không được để trống");
		}

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		Group group = groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền tạo ca làm việc");
		}

		return requests.stream().map(req -> createShiftInternal(group, groupId, req)).toList();
	}

	private CreateShiftResponse createShiftInternal(Group group, Long groupId, CreateShiftRequest request) {
		if (request.getDate() == null) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Ngày làm việc không được để trống");
		}

		ResolvedShiftInput resolved = resolveInput(groupId, request);

		List<Shift> overlappingShifts = shiftRepository.findOverlappingShifts(
				groupId,
				request.getDate(),
				resolved.startTime(),
				resolved.endTime()
		);

		if (!overlappingShifts.isEmpty()) {
			throw new BusinessException(HttpStatus.CONFLICT, "Ca làm việc bị trùng lặp thời gian với ca khác trong cùng ngày");
		}

		Shift shift = new Shift();
		shift.setGroup(group);
		shift.setTemplate(resolved.template());
		shift.setName(resolved.name());
		shift.setDate(request.getDate());
		shift.setStartTime(resolved.startTime());
		shift.setEndTime(resolved.endTime());
		shift.setNote(resolved.note());
		shift.setStatus(ShiftStatus.OPEN);

		Shift savedShift = shiftRepository.save(shift);

		// Auto-copy template requirements
		if (resolved.template() != null) {
			copyTemplateRequirements(savedShift, resolved.template().getId());
		}

		CreateShiftResponse response = new CreateShiftResponse();
		response.setId(savedShift.getId());
		response.setGroupId(groupId);
		response.setTemplateId(savedShift.getTemplate() != null ? savedShift.getTemplate().getId() : null);
		response.setName(savedShift.getName());
		response.setDate(savedShift.getDate());
		response.setStartTime(savedShift.getStartTime());
		response.setEndTime(savedShift.getEndTime());
		response.setNote(savedShift.getNote());
		response.setStatus(savedShift.getStatus());

		return response;
	}

	private ResolvedShiftInput resolveInput(Long groupId, CreateShiftRequest request) {
		String name = normalizeBlankToNull(request.getName());
		String note = normalizeBlankToNull(request.getNote());

		ShiftTemplate template = null;
		if (request.getTemplateId() != null) {
			template = shiftTemplateRepository.findByIdAndGroupId(request.getTemplateId(), groupId)
					.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca mẫu"));
			if (name == null) {
				name = template.getName();
			}
		}

		LocalTime startTime = request.getStartTime();
		LocalTime endTime = request.getEndTime();

		if (template != null) {
			if (startTime != null || endTime != null) {
				throw new BusinessException(HttpStatus.BAD_REQUEST, "Đã chọn ca mẫu thì không được nhập giờ thủ công");
			}
			startTime = template.getStartTime();
			endTime = template.getEndTime();
		} else {
			if (startTime == null || endTime == null) {
				throw new BusinessException(HttpStatus.BAD_REQUEST, "Giờ bắt đầu và giờ kết thúc không được để trống");
			}
		}

		if (!startTime.isBefore(endTime)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Giờ bắt đầu phải trước giờ kết thúc");
		}

		return new ResolvedShiftInput(template, name, startTime, endTime, note);
	}

	private void copyTemplateRequirements(Shift shift, Long templateId) {
		List<TemplateRequirement> tplReqs = templateRequirementRepository.findByTemplateId(templateId);
		for (TemplateRequirement tr : tplReqs) {
			ShiftRequirement sr = new ShiftRequirement();
			sr.setShift(shift);
			sr.setPosition(tr.getPosition());
			sr.setQuantity(tr.getQuantity());
			shiftRequirementRepository.save(sr);
		}
	}

	@Transactional
	public void deleteShift(Long groupId, String username, Long shiftId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền xóa ca làm việc");
		}

		Shift shift = shiftRepository.findById(shiftId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

		if (!shift.getGroup().getId().equals(groupId)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Ca làm việc không thuộc nhóm này");
		}

		// Cascade delete related data
		registrationRepository.deleteByShiftId(shiftId);
		shiftRequirementRepository.deleteAllByShiftIdIn(java.util.List.of(shiftId));
		shiftRepository.delete(shift);
	}

	/**
	 * B20: Khóa Ca – chuyển trạng thái OPEN → LOCKED.
	 * Tự động REJECT tất cả Registration PENDING còn lại của ca.
	 */
	@Transactional
	public CreateShiftResponse lockShift(Long groupId, Long shiftId, String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền khóa ca làm việc");
		}

		Shift shift = shiftRepository.findByIdAndGroupId(shiftId, groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

		if (shift.getStatus() != ShiftStatus.OPEN) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Chỉ có thể khóa ca đang ở trạng thái OPEN");
		}

		shift.setStatus(ShiftStatus.LOCKED);
		shiftRepository.save(shift);

		// Tự động REJECT tất cả Registration PENDING
		List<Registration> pending = registrationRepository.findByShiftIdAndStatus(shiftId, RegistrationStatus.PENDING);
		for (Registration reg : pending) {
			reg.setStatus(RegistrationStatus.REJECTED);
			reg.setManagerNote("Ca đã bị khóa");
		}
		registrationRepository.saveAll(pending);

		return toResponse(shift);
	}

	private String normalizeBlankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record ResolvedShiftInput(
			ShiftTemplate template,
			String name,
			LocalTime startTime,
			LocalTime endTime,
			String note
	) {
	}

	@Transactional(readOnly = true)
	public List<AvailableShiftResponse> getAvailableShifts(Long groupId, String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản của bạn chưa được duyệt trong nhóm này");
		}

		List<Availability> availabilities = availabilityRepository.findByUserId(user.getId());

		List<Shift> openShifts = shiftRepository
				.findByGroupIdAndStatusOrderByDateAscStartTimeAsc(groupId, ShiftStatus.OPEN);

		return openShifts.stream()
				.filter(shift -> matchesAvailability(shift, availabilities))
				.filter(shift -> hasAvailableSlots(shift))
				.map(shift -> toAvailableShiftResponse(shift, groupId))
				.toList();
	}

	/**
	 * Kiểm tra ca có khớp với ít nhất 1 khung availability của member không.
	 * Điều kiện: đúng ngày trong tuần VÀ ca nằm trong (hoặc overlap) khung giờ availability.
	 * Overlap: avail.startTime <= shift.startTime AND avail.endTime >= shift.endTime
	 */
	private boolean matchesAvailability(Shift shift, List<Availability> availabilities) {
		if (availabilities.isEmpty()) {
			return false;
		}
		return availabilities.stream().anyMatch(avail ->
				avail.getDayOfWeek() == shift.getDate().getDayOfWeek()
						&& !avail.getStartTime().isAfter(shift.getStartTime())
						&& !avail.getEndTime().isBefore(shift.getEndTime())
		);
	}

	/**
	 * Kiểm tra ca có ít nhất 1 ShiftRequirement với tổng slot > 0.
	 * TODO: Khi B12 (đăng ký ca) được implement, cập nhật logic để trừ số slot đã đăng ký.
	 */
	private boolean hasAvailableSlots(Shift shift) {
		List<ShiftRequirement> requirements = shiftRequirementRepository.findByShiftId(shift.getId());
		return requirements.stream().anyMatch(req -> req.getQuantity() > 0);
	}

	private AvailableShiftResponse toAvailableShiftResponse(Shift shift, Long groupId) {
		List<ShiftRequirement> requirements = shiftRequirementRepository.findByShiftId(shift.getId());
		int totalSlots = requirements.stream().mapToInt(ShiftRequirement::getQuantity).sum();

		AvailableShiftResponse response = new AvailableShiftResponse();
		response.setId(shift.getId());
		response.setGroupId(groupId);
		response.setName(shift.getName());
		response.setDate(shift.getDate());
		response.setStartTime(shift.getStartTime());
		response.setEndTime(shift.getEndTime());
		response.setNote(shift.getNote());
		response.setStatus(shift.getStatus());
		response.setTotalSlots(totalSlots);
		return response;
	}
}
