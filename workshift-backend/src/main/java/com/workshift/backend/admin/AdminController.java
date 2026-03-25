package com.workshift.backend.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.workshift.backend.admin.dto.AdminUserResponse;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping("/ping")
	public ResponseEntity<ApiResponse<String>> pingAdmin() {
		return ResponseEntity.ok(ApiResponse.ok("pong", "ADMIN connection successful"));
	}

	@GetMapping("/users")
	public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String search) {
		
		Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
		Page<AdminUserResponse> result = adminService.getUsers(search, pageable);
		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách người dùng thành công", result));
	}

	@PatchMapping("/users/{userId}/toggle-status")
	public ResponseEntity<ApiResponse<AdminUserResponse>> toggleUserStatus(@PathVariable("userId") Long userId) {
		AdminUserResponse result = adminService.toggleUserStatus(userId);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái người dùng thành công", result));
	}

	@GetMapping("/groups")
	public ResponseEntity<ApiResponse<Page<com.workshift.backend.admin.dto.AdminGroupResponse>>> getGroups(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String search) {
		
		Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
		Page<com.workshift.backend.admin.dto.AdminGroupResponse> result = adminService.getGroups(search, pageable);
		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách nhóm thành công", result));
	}

	@PatchMapping("/groups/{groupId}/toggle-status")
	public ResponseEntity<ApiResponse<com.workshift.backend.admin.dto.AdminGroupResponse>> toggleGroupStatus(@PathVariable("groupId") Long groupId) {
		com.workshift.backend.admin.dto.AdminGroupResponse result = adminService.toggleGroupStatus(groupId);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái nhóm thành công", result));
	}

	@GetMapping("/metrics")
	public ResponseEntity<ApiResponse<com.workshift.backend.admin.dto.SystemMetricsResponse>> getSystemMetrics() {
		com.workshift.backend.admin.dto.SystemMetricsResponse metrics = adminService.getSystemMetrics();
		return ResponseEntity.ok(ApiResponse.ok("Lấy dữ liệu thống kê thành công", metrics));
	}

	@GetMapping("/audit-logs")
	public ResponseEntity<ApiResponse<Page<com.workshift.backend.admin.dto.AdminAuditLogResponse>>> getAuditLogs(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		
		Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
		Page<com.workshift.backend.admin.dto.AdminAuditLogResponse> result = adminService.getAuditLogs(pageable);
		return ResponseEntity.ok(ApiResponse.ok("Lấy lịch sử thao tác thành công", result));
	}
}
