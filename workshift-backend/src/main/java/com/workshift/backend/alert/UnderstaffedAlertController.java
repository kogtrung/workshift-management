package com.workshift.backend.alert;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.alert.dto.UnderstaffedAlertResponse;
import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/alerts")
public class UnderstaffedAlertController {

	private final UnderstaffedAlertService understaffedAlertService;

	public UnderstaffedAlertController(UnderstaffedAlertService understaffedAlertService) {
		this.understaffedAlertService = understaffedAlertService;
	}

	@GetMapping("/understaffed")
	public ResponseEntity<ApiResponse<List<UnderstaffedAlertResponse>>> getUnderstaffedShifts(
			Authentication authentication,
			@PathVariable("groupId") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<UnderstaffedAlertResponse> data = understaffedAlertService
				.getUnderstaffedShifts(groupId, authentication.getName());
		return ResponseEntity.ok(ApiResponse.ok("Danh sách ca thiếu người", data));
	}
}
