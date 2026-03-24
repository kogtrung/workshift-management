package com.workshift.backend.shift;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.shift.dto.AvailableShiftResponse;
import com.workshift.backend.shift.dto.CreateShiftBulkRequest;
import com.workshift.backend.shift.dto.CreateShiftRequest;
import com.workshift.backend.shift.dto.CreateShiftResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/shifts")
public class ShiftController {

	private final ShiftService shiftService;

	public ShiftController(ShiftService shiftService) {
		this.shiftService = shiftService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<CreateShiftResponse>>> getShifts(
			Authentication authentication,
			@PathVariable("groupId") Long groupId,
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<CreateShiftResponse> data = shiftService.getShifts(groupId, authentication.getName(), from, to);
		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách ca làm việc thành công", data));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CreateShiftResponse>> createShift(
			Authentication authentication,
			@PathVariable("groupId") Long groupId,
			@Valid @RequestBody CreateShiftRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		CreateShiftResponse data = shiftService.createShift(groupId, authentication.getName(), request);
		return ResponseEntity.status(201).body(ApiResponse.created("Tạo ca làm việc thành công", data));
	}

	@PostMapping("/bulk")
	public ResponseEntity<ApiResponse<java.util.List<CreateShiftResponse>>> createShiftBulk(
			Authentication authentication,
			@PathVariable("groupId") Long groupId,
			@Valid @RequestBody CreateShiftBulkRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		var data = shiftService.createShiftsBulk(groupId, authentication.getName(), request.shifts());
		return ResponseEntity.status(201).body(ApiResponse.created("Tạo ca làm việc hàng loạt thành công", data));
	}

	@GetMapping("/available")
	public ResponseEntity<ApiResponse<List<AvailableShiftResponse>>> getAvailableShifts(
			Authentication authentication,
			@PathVariable("groupId") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<AvailableShiftResponse> data = shiftService.getAvailableShifts(groupId, authentication.getName());
		return ResponseEntity.ok(ApiResponse.ok("Danh sách ca phù hợp", data));
	}
}

