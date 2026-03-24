package com.workshift.backend.registration;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.registration.dto.RegisterShiftRequest;
import com.workshift.backend.registration.dto.RegistrationResponse;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;

@Service
@Transactional
public class RegistrationService {

	private final RegistrationRepository registrationRepository;
	private final ShiftRepository shiftRepository;
	private final UserRepository userRepository;
	private final PositionRepository positionRepository;

	public RegistrationService(RegistrationRepository registrationRepository,
	                           ShiftRepository shiftRepository,
	                           UserRepository userRepository,
	                           PositionRepository positionRepository) {
		this.registrationRepository = registrationRepository;
		this.shiftRepository = shiftRepository;
		this.userRepository = userRepository;
		this.positionRepository = positionRepository;
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

		return new RegistrationResponse(
				registration.getId(),
				shift.getId(),
				user.getId(),
				position.getId(),
				registration.getStatus(),
				registration.getNote()
		);
	}
}
