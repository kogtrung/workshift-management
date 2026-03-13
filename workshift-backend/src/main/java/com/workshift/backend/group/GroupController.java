package com.workshift.backend.group;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.group.dto.CreateGroupRequest;
import com.workshift.backend.group.dto.CreateGroupResponse;
import com.workshift.backend.group.dto.GroupMemberResponse;
import com.workshift.backend.group.dto.JoinGroupByCodeRequest;
import com.workshift.backend.group.dto.JoinGroupResponse;
import com.workshift.backend.group.dto.ReviewGroupMemberRequest;

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

	@GetMapping("/{id}/members/pending")
	public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getPendingMembers(
			Authentication authentication,
			@PathVariable("id") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<GroupMemberResponse> data = groupService.getPendingMembers(authentication.getName(), groupId);
		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách yêu cầu tham gia thành công", data));
	}

	@PatchMapping("/{id}/members/{memberId}")
	public ResponseEntity<ApiResponse<GroupMemberResponse>> reviewMember(
			Authentication authentication,
			@PathVariable("id") Long groupId,
			@PathVariable("memberId") Long memberId,
			@Valid @RequestBody ReviewGroupMemberRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		GroupMemberResponse data = groupService.reviewMember(authentication.getName(), groupId, memberId, request);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành viên thành công", data));
	}
}
