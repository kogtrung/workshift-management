package com.workshift.backend.registration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.registration.dto.RegisterShiftRequest;
import com.workshift.backend.registration.dto.RegistrationResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shifts")
public class RegistrationController {

	private final RegistrationService registrationService;

	public RegistrationController(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@PostMapping("/{id}/register")
	public ResponseEntity<ApiResponse<RegistrationResponse>> registerShift(
			Authentication authentication,
			@PathVariable("id") Long shiftId,
			@Valid @RequestBody RegisterShiftRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		RegistrationResponse data = registrationService.registerShift(shiftId, authentication.getName(), request);
		return ResponseEntity.status(201).body(ApiResponse.created("Đăng ký ca làm việc thành công", data));
	}
}
