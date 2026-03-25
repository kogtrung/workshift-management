package com.workshift.backend.me;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.me.dto.MyCalendarItemResponse;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class MeService {

	private final UserRepository userRepository;
	private final RegistrationRepository registrationRepository;

	public MeService(UserRepository userRepository, RegistrationRepository registrationRepository) {
		this.userRepository = userRepository;
		this.registrationRepository = registrationRepository;
	}

	@Transactional(readOnly = true)
	public List<MyCalendarItemResponse> getMyCalendar(String username, LocalDate from, LocalDate to) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		return registrationRepository.findByUserIdAndStatusAndShiftDateBetween(
				user.getId(),
				RegistrationStatus.APPROVED,
				from,
				to
		).stream().map(this::toResponse).toList();
	}

	private MyCalendarItemResponse toResponse(Registration registration) {
		var shift = registration.getShift();
		var group = shift.getGroup();
		var position = registration.getPosition();

		return new MyCalendarItemResponse(
				registration.getId(),
				group.getId(),
				group.getName(),
				shift.getId(),
				shift.getName(),
				shift.getDate(),
				shift.getStartTime(),
				shift.getEndTime(),
				shift.getStatus(),
				position.getId(),
				position.getName(),
				position.getColorCode(),
				registration.getStatus()
		);
	}
}

