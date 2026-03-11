package com.workshift.backend.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.workshift.backend.auth.dto.LoginRequest;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class AuthRefreshTokenTest {
	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void refresh_shouldReturnNewAccessAndRefreshToken() {
		User user = new User();
		user.setUsername("refresh_user");
		user.setEmail("refresh_user@example.com");
		user.setPassword(passwordEncoder.encode("secret123"));
		user.setFullName("Refresh User");
		userRepository.save(user);

		var loginResponse = authService.login(new LoginRequest("refresh_user", "secret123"));
		assertNotNull(loginResponse.refreshToken());

		var refreshResponse = authService.refresh(loginResponse.refreshToken());
		assertNotNull(refreshResponse.token());
		assertNotNull(refreshResponse.refreshToken());
	}

	@Test
	void refresh_shouldRejectOldRefreshTokenAfterRotation() {
		User user = new User();
		user.setUsername("refresh_rotate");
		user.setEmail("refresh_rotate@example.com");
		user.setPassword(passwordEncoder.encode("secret123"));
		user.setFullName("Refresh Rotate");
		userRepository.save(user);

		var loginResponse = authService.login(new LoginRequest("refresh_rotate", "secret123"));
		authService.refresh(loginResponse.refreshToken());

		assertThrows(BusinessException.class, () -> authService.refresh(loginResponse.refreshToken()));
	}
}
