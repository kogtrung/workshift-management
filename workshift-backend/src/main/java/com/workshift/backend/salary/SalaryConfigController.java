package com.workshift.backend.salary;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.salary.dto.SalaryConfigRequest;
import com.workshift.backend.salary.dto.SalaryConfigResponse;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/salary-configs")
public class SalaryConfigController {

    private final SalaryConfigService salaryConfigService;

    public SalaryConfigController(SalaryConfigService salaryConfigService) {
        this.salaryConfigService = salaryConfigService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SalaryConfigResponse>> createConfig(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @Valid @RequestBody SalaryConfigRequest request
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        SalaryConfigResponse data = salaryConfigService.createSalaryConfig(groupId, authentication.getName(), request);
        return ResponseEntity.status(201).body(ApiResponse.created("Tạo cấu hình lương thành công", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalaryConfigResponse>>> getConfigs(
            Authentication authentication,
            @PathVariable("groupId") Long groupId
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        List<SalaryConfigResponse> data = salaryConfigService.getConfigs(groupId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách cấu hình lương thành công", data));
    }

    @DeleteMapping("/{configId}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @PathVariable("configId") Long configId
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        salaryConfigService.deleteConfig(groupId, configId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Xóa cấu hình lương thành công", null));
    }
}
