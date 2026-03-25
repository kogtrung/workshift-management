package com.workshift.backend.availability;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.availability.dto.AvailabilityResponse;
import com.workshift.backend.availability.dto.UpdateAvailabilityRequest;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Availability;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.AvailabilityRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class AvailabilityService {

	private final AvailabilityRepository availabilityRepository;
	private final UserRepository userRepository;

	public AvailabilityService(AvailabilityRepository availabilityRepository, UserRepository userRepository) {
		this.availabilityRepository = availabilityRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<AvailabilityResponse> getMyAvailability(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		return availabilityRepository.findByUserId(user.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public List<AvailabilityResponse> updateMyAvailability(String username, UpdateAvailabilityRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		// Xóa toàn bộ lịch cũ của User
		availabilityRepository.deleteByUserId(user.getId());

		// Validate và map các lịch tĩnh mới
		List<Availability> newAvailabilities = request.items().stream()
				.map(item -> {
					if (item.endTime().isBefore(item.startTime()) || item.endTime().equals(item.startTime())) {
						throw new BusinessException(HttpStatus.BAD_REQUEST, "Giờ kết thúc phải sau giờ bắt đầu");
					}
					Availability availability = new Availability();
					availability.setUser(user);
					availability.setDayOfWeek(item.dayOfWeek());
					availability.setStartTime(item.startTime());
					availability.setEndTime(item.endTime());
					return availability;
				})
				.toList();

		// Lưu mảng mới vào Database
		List<Availability> saved = availabilityRepository.saveAll(newAvailabilities);

		return saved.stream()
				.map(this::toResponse)
				.toList();
	}

	private AvailabilityResponse toResponse(Availability availability) {
		return new AvailabilityResponse(
				availability.getId(),
				availability.getDayOfWeek(),
				availability.getStartTime(),
				availability.getEndTime()
		);
	}
}
