package com.workshift.backend.availability;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.availability.dto.AvailabilityResponse;
import com.workshift.backend.availability.dto.UpdateAvailabilityRequest;
import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/availability")
public class AvailabilityController {

	private final AvailabilityService availabilityService;

	public AvailabilityController(AvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getAvailability(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<AvailabilityResponse> responses = availabilityService.getMyAvailability(authentication.getName());
		return ResponseEntity.ok(ApiResponse.ok("Lấy lịch rảnh thành công", responses));
	}

	@PutMapping
	public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> updateAvailability(
			Authentication authentication,
			@Valid @RequestBody UpdateAvailabilityRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<AvailabilityResponse> responses = availabilityService.updateMyAvailability(authentication.getName(), request);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật lịch rảnh thành công", responses));
	}
}
