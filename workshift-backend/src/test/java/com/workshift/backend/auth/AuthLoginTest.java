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
class AuthLoginTest {
	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void login_shouldReturnJwtToken() {
		User user = new User();
		user.setUsername("user_login");
		user.setEmail("user_login@example.com");
		user.setPassword(passwordEncoder.encode("secret123"));
		user.setFullName("User Login");
		userRepository.save(user);

		var response = authService.login(new LoginRequest("user_login@example.com", "secret123"));
		assertNotNull(response.token());
		assertNotNull(response.refreshToken());
	}

	@Test
	void login_shouldRejectWrongPassword() {
		User user = new User();
		user.setUsername("user_wrong");
		user.setEmail("user_wrong@example.com");
		user.setPassword(passwordEncoder.encode("secret123"));
		user.setFullName("User Wrong");
		userRepository.save(user);

		assertThrows(BusinessException.class, () -> authService.login(new LoginRequest("user_wrong", "wrong")));
	}
}
