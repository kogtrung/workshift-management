package com.workshift.backend.memberposition;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.memberposition.dto.MemberPositionResponse;
import com.workshift.backend.memberposition.dto.UpdateMemberPositionsRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/my-positions")
public class MemberPositionController {

	private final MemberPositionService memberPositionService;

	public MemberPositionController(MemberPositionService memberPositionService) {
		this.memberPositionService = memberPositionService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<MemberPositionResponse>>> getMyPositions(
			Authentication authentication,
			@PathVariable("groupId") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<MemberPositionResponse> data = memberPositionService.getMyPositions(authentication.getName(), groupId);
		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách vị trí của tôi", data));
	}

	@PutMapping
	public ResponseEntity<ApiResponse<List<MemberPositionResponse>>> updateMyPositions(
			Authentication authentication,
			@PathVariable("groupId") Long groupId,
			@Valid @RequestBody UpdateMemberPositionsRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<MemberPositionResponse> data = memberPositionService.updateMyPositions(
				authentication.getName(), groupId, request.positionIds()
		);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật vị trí thành công", data));
	}
}
