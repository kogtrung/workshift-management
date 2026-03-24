package com.workshift.backend.registration;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftRequirement;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.registration.dto.ApproveRegistrationRequest;
import com.workshift.backend.registration.dto.RegisterShiftRequest;
import com.workshift.backend.registration.dto.RegistrationResponse;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.ShiftRequirementRepository;
import com.workshift.backend.repository.UserRepository;

@Service
@Transactional
public class RegistrationService {

	private final RegistrationRepository registrationRepository;
	private final ShiftRepository shiftRepository;
	private final UserRepository userRepository;
	private final PositionRepository positionRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final ShiftRequirementRepository shiftRequirementRepository;

	public RegistrationService(RegistrationRepository registrationRepository,
	                           ShiftRepository shiftRepository,
	                           UserRepository userRepository,
	                           PositionRepository positionRepository,
	                           GroupMemberRepository groupMemberRepository,
	                           ShiftRequirementRepository shiftRequirementRepository) {
		this.registrationRepository = registrationRepository;
		this.shiftRepository = shiftRepository;
		this.userRepository = userRepository;
		this.positionRepository = positionRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.shiftRequirementRepository = shiftRequirementRepository;
	}

	public RegistrationResponse registerShift(Long shiftId, String username, RegisterShiftRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

		Shift shift = shiftRepository.findById(shiftId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

		if (shift.getStatus() == ShiftStatus.LOCKED || shift.getStatus() == ShiftStatus.COMPLETED) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Không thể đăng ký ca đã khóa hoặc hoàn thành");
		}

		Position position = positionRepository.findById(request.getPositionId())
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy vị trí"));

		if (!position.getGroup().getId().equals(shift.getGroup().getId())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Vị trí không thuộc nhóm của ca làm việc");
		}

		if (registrationRepository.existsByShiftAndUser(shift, user)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Bạn đã đăng ký ca này rồi");
		}

		Registration registration = new Registration();
		registration.setShift(shift);
		registration.setUser(user);
		registration.setPosition(position);
		registration.setStatus(RegistrationStatus.PENDING);
		registration.setNote(request.getNote());

		registration = registrationRepository.save(registration);

		return toResponse(registration);
	}

	public RegistrationResponse cancelRegistration(Long registrationId, String username, com.workshift.backend.registration.dto.CancelRegistrationRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

		Registration registration = registrationRepository.findByIdAndUser(registrationId, user)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký của bạn"));

		Shift shift = registration.getShift();

		if (shift.getStatus() == ShiftStatus.LOCKED || shift.getStatus() == ShiftStatus.COMPLETED) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Không thể hủy khi ca làm việc đã khóa hoặc hoàn thành");
		}

		java.time.LocalDateTime now = java.time.LocalDateTime.now();
		java.time.LocalDateTime shiftStart = java.time.LocalDateTime.of(shift.getDate(), shift.getStartTime());

		if (!now.isBefore(shiftStart)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Đã quá hạn hủy ca làm việc");
		}

		registration.setStatus(RegistrationStatus.CANCELLED);
		
		if (request != null && request.getReason() != null && !request.getReason().trim().isEmpty()) {
			registration.setNote(request.getReason());
		}

		registration = registrationRepository.save(registration);

		return toResponse(registration);
	}

	/**
	 * B14: Manager duyệt đăng ký ca.
	 * Kiểm tra: PENDING, quyền MANAGER, ca chưa LOCKED, còn slot theo ShiftRequirement.
	 */
	public RegistrationResponse approveRegistration(Long registrationId, String username, ApproveRegistrationRequest request) {
		// 1. Xác thực manager
		User manager = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		// 2. Tìm registration
		Registration registration = registrationRepository.findById(registrationId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký"));

		// 3. Kiểm tra trạng thái PENDING
		if (registration.getStatus() != RegistrationStatus.PENDING) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Chỉ có thể duyệt đăng ký ở trạng thái chờ duyệt");
		}

		Shift shift = registration.getShift();
		Long groupId = shift.getGroup().getId();

		// 4. Kiểm tra quyền MANAGER trong group
		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, manager.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền duyệt đăng ký ca");
		}

		// 5. Kiểm tra ca chưa LOCKED/COMPLETED
		if (shift.getStatus() == ShiftStatus.LOCKED || shift.getStatus() == ShiftStatus.COMPLETED) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Không thể duyệt đăng ký cho ca đã khóa hoặc hoàn thành");
		}

		// 6. Kiểm tra quota ShiftRequirement
		Long positionId = registration.getPosition().getId();

		ShiftRequirement requirement = shiftRequirementRepository
				.findByShiftIdAndPositionId(shift.getId(), positionId)
				.orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
						"Ca làm việc chưa có nhu cầu cho vị trí này"));

		long approvedCount = registrationRepository.countByShiftIdAndPositionIdAndStatus(
				shift.getId(), positionId, RegistrationStatus.APPROVED);

		if (approvedCount >= requirement.getQuantity()) {
			throw new BusinessException(HttpStatus.CONFLICT,
					"Vị trí này đã đủ số lượng nhân viên được duyệt (tối đa " + requirement.getQuantity() + ")");
		}

		// 7. Cập nhật trạng thái
		registration.setStatus(RegistrationStatus.APPROVED);

		if (request != null && request.getManagerNote() != null && !request.getManagerNote().trim().isEmpty()) {
			registration.setManagerNote(request.getManagerNote());
		}

		registration = registrationRepository.save(registration);

		return toResponse(registration);
	}

	/**
	 * B15: Manager từ chối đăng ký ca.
	 * Kiểm tra: PENDING, quyền MANAGER. Cập nhật trạng thái REJECTED và lý do.
	 */
	public RegistrationResponse rejectRegistration(Long registrationId, String username, com.workshift.backend.registration.dto.RejectRegistrationRequest request) {
		User manager = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		Registration registration = registrationRepository.findById(registrationId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký"));

		if (registration.getStatus() != RegistrationStatus.PENDING) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Chỉ có thể từ chối đăng ký ở trạng thái chờ duyệt");
		}

		Shift shift = registration.getShift();
		Long groupId = shift.getGroup().getId();

		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, manager.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền từ chối đăng ký ca");
		}

		registration.setStatus(RegistrationStatus.REJECTED);
		registration.setManagerNote(request.getReason());

		registration = registrationRepository.save(registration);

		return toResponse(registration);
	}

	/**
	 * Danh sách đăng ký PENDING theo ca (cho Manager xem).
	 */
	@Transactional(readOnly = true)
	public List<RegistrationResponse> getPendingRegistrations(Long shiftId, String username) {
		User manager = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		Shift shift = shiftRepository.findById(shiftId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

		Long groupId = shift.getGroup().getId();

		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, manager.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền xem danh sách đăng ký");
		}

		return registrationRepository.findByShiftIdAndStatus(shiftId, RegistrationStatus.PENDING)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	private RegistrationResponse toResponse(Registration registration) {
		return new RegistrationResponse(
				registration.getId(),
				registration.getShift().getId(),
				registration.getUser().getId(),
				registration.getPosition().getId(),
				registration.getStatus(),
				registration.getNote(),
				registration.getManagerNote()
		);
	}
}
