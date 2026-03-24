package com.workshift.backend.position;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.position.dto.CreatePositionRequest;
import com.workshift.backend.position.dto.PositionResponse;
import com.workshift.backend.position.dto.UpdatePositionRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/positions")
public class PositionController {

	private final PositionService positionService;

	public PositionController(PositionService positionService) {
		this.positionService = positionService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PositionResponse>> createPosition(
			Authentication authentication,
			@PathVariable Long groupId,
			@Valid @RequestBody CreatePositionRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		PositionResponse response = positionService.createPosition(authentication.getName(), groupId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Tạo vị trí thành công", response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<PositionResponse>>> getPositions(
			Authentication authentication,
			@PathVariable Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<PositionResponse> responses = positionService.getPositions(authentication.getName(), groupId);
		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách vị trí thành công", responses));
	}

	@PutMapping("/{positionId}")
	public ResponseEntity<ApiResponse<PositionResponse>> updatePosition(
			Authentication authentication,
			@PathVariable Long groupId,
			@PathVariable Long positionId,
			@Valid @RequestBody UpdatePositionRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		PositionResponse response = positionService.updatePosition(authentication.getName(), groupId, positionId, request);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật vị trí thành công", response));
	}

	@DeleteMapping("/{positionId}")
	public ResponseEntity<ApiResponse<Void>> deletePosition(
			Authentication authentication,
			@PathVariable Long groupId,
			@PathVariable Long positionId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		positionService.deletePosition(authentication.getName(), groupId, positionId);
		return ResponseEntity.ok(ApiResponse.ok("Xóa vị trí thành công", null));
	}
}
