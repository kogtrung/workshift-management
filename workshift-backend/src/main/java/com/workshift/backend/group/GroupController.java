package com.workshift.backend.group;

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
import com.workshift.backend.group.dto.CreateGroupRequest;
import com.workshift.backend.group.dto.CreateGroupResponse;
import com.workshift.backend.group.dto.JoinGroupByCodeRequest;
import com.workshift.backend.group.dto.JoinGroupResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {
	private final GroupService groupService;

	public GroupController(GroupService groupService) {
		this.groupService = groupService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CreateGroupResponse>> createGroup(
			Authentication authentication,
			@Valid @RequestBody CreateGroupRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		CreateGroupResponse data = groupService.createGroup(authentication.getName(), request);
		return ResponseEntity.status(201).body(ApiResponse.created("Tạo group thành công", data));
	}

	@PostMapping("/{id}/join")
	public ResponseEntity<ApiResponse<JoinGroupResponse>> joinGroup(Authentication authentication, @PathVariable("id") Long groupId) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		JoinGroupResponse data = groupService.joinGroup(authentication.getName(), groupId);
		return ResponseEntity.status(201).body(ApiResponse.created("Gửi yêu cầu tham gia group thành công", data));
	}

	@PostMapping("/join-by-code")
	public ResponseEntity<ApiResponse<JoinGroupResponse>> joinGroupByCode(
			Authentication authentication,
			@Valid @RequestBody JoinGroupByCodeRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		JoinGroupResponse data = groupService.joinGroupByCode(authentication.getName(), request.joinCode());
		return ResponseEntity.status(201).body(ApiResponse.created("Gửi yêu cầu tham gia group thành công", data));
	}
}
