package com.workshift.backend.shift;

import java.time.LocalTime;
import java.util.List;

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

@Service
public class ShiftService {

	private final ShiftRepository shiftRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final UserRepository userRepository;
	private final ShiftTemplateRepository shiftTemplateRepository;
	private final AvailabilityRepository availabilityRepository;
	private final ShiftRequirementRepository shiftRequirementRepository;

	public ShiftService(ShiftRepository shiftRepository,
						GroupRepository groupRepository,
						GroupMemberRepository groupMemberRepository,
						UserRepository userRepository,
						ShiftTemplateRepository shiftTemplateRepository,
						AvailabilityRepository availabilityRepository,
						ShiftRequirementRepository shiftRequirementRepository) {
		this.shiftRepository = shiftRepository;
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.userRepository = userRepository;
		this.shiftTemplateRepository = shiftTemplateRepository;
		this.availabilityRepository = availabilityRepository;
		this.shiftRequirementRepository = shiftRequirementRepository;
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
