package com.workshift.backend.registration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.registration.dto.CancelRegistrationRequest;
import com.workshift.backend.registration.dto.RegistrationResponse;

@RestController
@RequestMapping("/api/v1/registrations")
public class MemberRegistrationController {

    private final RegistrationService registrationService;

    public MemberRegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RegistrationResponse>> cancelRegistration(
            Authentication authentication,
            @PathVariable("id") Long registrationId,
            @RequestBody(required = false) CancelRegistrationRequest request
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
        }

        RegistrationResponse data = registrationService.cancelRegistration(
                registrationId, 
                authentication.getName(), 
                request
        );

        return ResponseEntity.ok(ApiResponse.ok("Hủy đăng ký ca làm việc thành công", data));
    }
}
