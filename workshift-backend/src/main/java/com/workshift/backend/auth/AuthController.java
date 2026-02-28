package com.workshift.backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.auth.dto.RegisterRequest;
import com.workshift.backend.auth.dto.RegisterResponse;
import com.workshift.backend.common.api.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
		RegisterResponse data = authService.register(request);
		return ResponseEntity.status(201).body(ApiResponse.created("Đăng ký thành công", data));
	}
}
