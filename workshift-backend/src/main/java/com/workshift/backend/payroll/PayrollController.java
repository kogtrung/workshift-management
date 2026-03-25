package com.workshift.backend.payroll;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.payroll.dto.PayrollResponse;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/payroll")
public class PayrollController {

	private final PayrollService payrollService;

	public PayrollController(PayrollService payrollService) {
		this.payrollService = payrollService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PayrollResponse>> getPayroll(
			Authentication authentication,
			@PathVariable("groupId") Long groupId,
			@RequestParam("month") int month,
			@RequestParam("year") int year
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		PayrollResponse data = payrollService.getPayroll(groupId, authentication.getName(), month, year);
		return ResponseEntity.ok(ApiResponse.ok("Bảng lương tháng " + month + "/" + year, data));
	}
}
