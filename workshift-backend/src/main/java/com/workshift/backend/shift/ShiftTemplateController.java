package com.workshift.backend.shift;

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
import com.workshift.backend.shift.dto.template.CreateShiftTemplateRequest;
import com.workshift.backend.shift.dto.template.ShiftTemplateResponse;
import com.workshift.backend.shift.dto.template.UpdateShiftTemplateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/shift-templates")
public class ShiftTemplateController {

	private final ShiftTemplateService shiftTemplateService;

	public ShiftTemplateController(ShiftTemplateService shiftTemplateService) {
		this.shiftTemplateService = shiftTemplateService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ShiftTemplateResponse>> createTemplate(
			Authentication authentication,
			@PathVariable Long groupId,
			@Valid @RequestBody CreateShiftTemplateRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		ShiftTemplateResponse response = shiftTemplateService.createTemplate(authentication.getName(), groupId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Tạo ca mẫu thành công", response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ShiftTemplateResponse>>> getTemplates(
			Authentication authentication,
			@PathVariable Long groupId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		List<ShiftTemplateResponse> responses = shiftTemplateService.getTemplates(authentication.getName(), groupId);
		return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách ca mẫu thành công", responses));
	}

	@PutMapping("/{templateId}")
	public ResponseEntity<ApiResponse<ShiftTemplateResponse>> updateTemplate(
			Authentication authentication,
			@PathVariable Long groupId,
			@PathVariable Long templateId,
			@Valid @RequestBody UpdateShiftTemplateRequest request
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		ShiftTemplateResponse response = shiftTemplateService.updateTemplate(authentication.getName(), groupId, templateId, request);
		return ResponseEntity.ok(ApiResponse.ok("Cập nhật ca mẫu thành công", response));
	}

	@DeleteMapping("/{templateId}")
	public ResponseEntity<ApiResponse<Void>> deleteTemplate(
			Authentication authentication,
			@PathVariable Long groupId,
			@PathVariable Long templateId
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		shiftTemplateService.deleteTemplate(authentication.getName(), groupId, templateId);
		return ResponseEntity.ok(ApiResponse.ok("Xóa ca mẫu thành công", null));
	}
}
