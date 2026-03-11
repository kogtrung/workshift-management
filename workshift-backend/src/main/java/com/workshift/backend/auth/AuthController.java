package com.workshift.backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.auth.dto.LoginRequest;
import com.workshift.backend.auth.dto.LoginResponse;
import com.workshift.backend.auth.dto.RefreshTokenRequest;
import com.workshift.backend.auth.dto.RefreshTokenResponse;
import com.workshift.backend.auth.dto.RegisterRequest;
import com.workshift.backend.auth.dto.RegisterResponse;
import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

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

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		LoginResponse data = authService.login(request);
		return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", data));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		RefreshTokenResponse data = authService.refresh(request.refreshToken());
		return ResponseEntity.ok(ApiResponse.ok("Làm mới token thành công", data));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<String>> logout(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}
		authService.logout(authentication.getName());
		return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công", "OK"));
	}
}
