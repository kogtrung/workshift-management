package com.workshift.backend.group;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GroupAuditActionType;
import com.workshift.backend.domain.GroupAuditEntityType;
import com.workshift.backend.group.dto.GroupAuditDailySummaryResponse;
import com.workshift.backend.group.dto.GroupAuditLogPageResponse;
import com.workshift.backend.group.dto.GroupAuditMonthlySummaryResponse;
import com.workshift.backend.group.dto.CreateGroupRequest;
import com.workshift.backend.group.dto.CreateGroupResponse;
import com.workshift.backend.group.dto.MyGroupResponse;
import com.workshift.backend.group.dto.GroupMemberDetailResponse;
import com.workshift.backend.group.dto.GroupMemberResponse;
import com.workshift.backend.group.dto.JoinGroupByCodeRequest;
import com.workshift.backend.group.dto.JoinGroupResponse;
import com.workshift.backend.group.dto.ReviewGroupMemberRequest;
import com.workshift.backend.group.dto.UpdateGroupRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {
	private final GroupService groupService;
	private final GroupAuditService groupAuditService;

	public GroupController(GroupService groupService, GroupAuditService groupAuditService) {
		this.groupService = groupService;
		this.groupAuditService = groupAuditService;
	}

	@GetMapping("/my-groups")
	public ResponseEntity<ApiResponse<List<MyGroupResponse>>> getMyGroups(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<MyGroupResponse> data = groupService.getMyGroups(authentication.getName());
		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách group thành công", data));
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

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<CreateGroupResponse>> updateGroup(
			Authentication authentication,
			@PathVariable("id") Long groupId,
			@Valid @RequestBody UpdateGroupRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		CreateGroupResponse data = groupService.updateGroup(authentication.getName(), groupId, request);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật group thành công", data));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<ApiResponse<CreateGroupResponse>> toggleGroupStatus(
			Authentication authentication,
			@PathVariable("id") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		CreateGroupResponse data = groupService.toggleGroupStatus(authentication.getName(), groupId);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái group thành công", data));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteGroupPermanently(
			Authentication authentication,
			@PathVariable("id") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		groupService.deleteGroupPermanently(authentication.getName(), groupId);
		return ResponseEntity.ok(ApiResponse.ok("Xóa group vĩnh viễn thành công", null));
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

	@GetMapping("/{id}/members")
	public ResponseEntity<ApiResponse<List<GroupMemberDetailResponse>>> getMembers(
			Authentication authentication,
			@PathVariable("id") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<GroupMemberDetailResponse> data = groupService.getGroupMembers(authentication.getName(), groupId);
		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách thành viên thành công", data));
	}

	@DeleteMapping("/{id}/leave")
	public ResponseEntity<ApiResponse<Void>> leaveGroup(
			Authentication authentication,
			@PathVariable("id") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		groupService.leaveGroup(authentication.getName(), groupId);
		return ResponseEntity.ok(ApiResponse.ok("Rời group thành công", null));
	}

	@DeleteMapping("/{id}/members/me")
	public ResponseEntity<ApiResponse<Void>> leaveGroupByCurrentMember(
			Authentication authentication,
			@PathVariable("id") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		groupService.leaveGroup(authentication.getName(), groupId);
		return ResponseEntity.ok(ApiResponse.ok("Rời group thành công", null));
	}

	@GetMapping("/{id}/members/pending")
	public ResponseEntity<ApiResponse<List<GroupMemberDetailResponse>>> getPendingMembers(
			Authentication authentication,
			@PathVariable("id") Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<GroupMemberDetailResponse> data = groupService.getPendingMembers(authentication.getName(), groupId);
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

	@GetMapping("/{id}/audit-logs")
	public ResponseEntity<ApiResponse<GroupAuditLogPageResponse>> getAuditLogs(
			Authentication authentication,
			@PathVariable("id") Long groupId,
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(value = "actionType", required = false) GroupAuditActionType actionType,
			@RequestParam(value = "actorUserId", required = false) Long actorUserId,
			@RequestParam(value = "entityType", required = false) GroupAuditEntityType entityType,
			@RequestParam(value = "entityId", required = false) Long entityId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		GroupAuditLogPageResponse data = groupAuditService.getAuditLogs(
				authentication.getName(), groupId, from, to, actionType, actorUserId, entityType, entityId, page, size
		);
		return ResponseEntity.ok(ApiResponse.ok("Lấy nhật ký hoạt động group thành công", data));
	}

	@GetMapping("/{id}/audit-logs/summary/daily")
	public ResponseEntity<ApiResponse<GroupAuditDailySummaryResponse>> getDailyAuditSummary(
			Authentication authentication,
			@PathVariable("id") Long groupId,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		GroupAuditDailySummaryResponse data = groupAuditService.getDailySummary(authentication.getName(), groupId, date);
		return ResponseEntity.ok(ApiResponse.ok("Lấy tổng hợp hoạt động theo ngày thành công", data));
	}

	@GetMapping("/{id}/audit-logs/summary/monthly")
	public ResponseEntity<ApiResponse<GroupAuditMonthlySummaryResponse>> getMonthlyAuditSummary(
			Authentication authentication,
			@PathVariable("id") Long groupId,
			@RequestParam("month") int month,
			@RequestParam("year") int year
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		GroupAuditMonthlySummaryResponse data = groupAuditService.getMonthlySummary(authentication.getName(), groupId, month, year);
		return ResponseEntity.ok(ApiResponse.ok("Lấy tổng hợp hoạt động theo tháng thành công", data));
	}
}
