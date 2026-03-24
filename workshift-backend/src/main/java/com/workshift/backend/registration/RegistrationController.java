package com.workshift.backend.registration;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.registration.dto.ApproveRegistrationRequest;
import com.workshift.backend.registration.dto.RegisterShiftRequest;
import com.workshift.backend.registration.dto.RegistrationResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class RegistrationController {

	private final RegistrationService registrationService;

	public RegistrationController(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@PostMapping("/shifts/{id}/register")
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


	@PatchMapping("/registrations/{id}/approve")
	public ResponseEntity<ApiResponse<RegistrationResponse>> approveRegistration(
			Authentication authentication,
			@PathVariable("id") Long registrationId,
			@RequestBody(required = false) ApproveRegistrationRequest request

	@PostMapping("/{id}/assign")
	public ResponseEntity<ApiResponse<RegistrationResponse>> assignShift(
			Authentication authentication,
			@PathVariable("id") Long shiftId,
			@Valid @RequestBody com.workshift.backend.registration.dto.AssignShiftRequest request

	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}


		RegistrationResponse data = registrationService.approveRegistration(registrationId, authentication.getName(), request);
		return ResponseEntity.ok(ApiResponse.ok("Duyệt đăng ký ca thành công", data));
	}

	@PatchMapping("/registrations/{id}/reject")
	public ResponseEntity<ApiResponse<RegistrationResponse>> rejectRegistration(
			Authentication authentication,
			@PathVariable("id") Long registrationId,
			@Valid @RequestBody com.workshift.backend.registration.dto.RejectRegistrationRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		RegistrationResponse data = registrationService.rejectRegistration(registrationId, authentication.getName(), request);
		return ResponseEntity.ok(ApiResponse.ok("Từ chối đăng ký ca thành công", data));
	}

	@GetMapping("/shifts/{id}/registrations/pending")
	public ResponseEntity<ApiResponse<List<RegistrationResponse>>> getPendingRegistrations(
			Authentication authentication,
			@PathVariable("id") Long shiftId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<RegistrationResponse> data = registrationService.getPendingRegistrations(shiftId, authentication.getName());
		return ResponseEntity.ok(ApiResponse.ok("Danh sách đăng ký chờ duyệt", data));

		RegistrationResponse data = registrationService.assignShift(shiftId, authentication.getName(), request);
		return ResponseEntity.status(201).body(ApiResponse.created("Gán nhân viên vào ca thành công", data));

	}
}
