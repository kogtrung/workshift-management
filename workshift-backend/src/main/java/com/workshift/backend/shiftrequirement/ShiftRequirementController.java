package com.workshift.backend.shiftrequirement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.shiftrequirement.dto.ShiftRequirementResponse;
import com.workshift.backend.shiftrequirement.dto.UpsertShiftRequirementRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shifts/{shiftId}/requirements")
public class ShiftRequirementController {

	private final ShiftRequirementService shiftRequirementService;

	public ShiftRequirementController(ShiftRequirementService shiftRequirementService) {
		this.shiftRequirementService = shiftRequirementService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ShiftRequirementResponse>> createRequirement(
			Authentication authentication,
			@PathVariable("shiftId") Long shiftId,
			@Valid @RequestBody UpsertShiftRequirementRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		ShiftRequirementResponse data = shiftRequirementService.createRequirement(authentication.getName(), shiftId, request);
		return ResponseEntity.status(201).body(ApiResponse.created("Tạo cấu hình nhu cầu thành công", data));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ShiftRequirementResponse>>> getRequirements(
			Authentication authentication,
			@PathVariable("shiftId") Long shiftId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<ShiftRequirementResponse> data = shiftRequirementService.getRequirements(authentication.getName(), shiftId);
		return ResponseEntity.ok(ApiResponse.ok("Lấy cấu hình nhu cầu thành công", data));
	}

	@PatchMapping("/{requirementId}")
	public ResponseEntity<ApiResponse<ShiftRequirementResponse>> updateRequirement(
			Authentication authentication,
			@PathVariable("shiftId") Long shiftId,
			@PathVariable("requirementId") Long requirementId,
			@Valid @RequestBody UpsertShiftRequirementRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		ShiftRequirementResponse data = shiftRequirementService.updateRequirement(authentication.getName(), shiftId, requirementId, request);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật cấu hình nhu cầu thành công", data));
	}

	@DeleteMapping("/{requirementId}")
	public ResponseEntity<ApiResponse<Void>> deleteRequirement(
			Authentication authentication,
			@PathVariable("shiftId") Long shiftId,
			@PathVariable("requirementId") Long requirementId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		shiftRequirementService.deleteRequirement(authentication.getName(), shiftId, requirementId);
		return ResponseEntity.ok(ApiResponse.ok("Xóa cấu hình nhu cầu thành công", null));
	}
}

