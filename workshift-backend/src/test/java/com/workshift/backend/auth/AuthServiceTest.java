package com.workshift.backend.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.workshift.backend.auth.dto.RegisterRequest;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {
	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void register_shouldHashPasswordAndPersistUser() {
		var response = authService.register(new RegisterRequest(
				"user1",
				"user1@example.com",
				"secret123",
				"User One",
				"0123456789"
		));

		assertNotNull(response.id());
		User user = userRepository.findById(response.id()).orElseThrow();
		assertNotNull(user.getPassword());
		assertNotEquals("secret123", user.getPassword());
		assertTrue(passwordEncoder.matches("secret123", user.getPassword()));
	}

	@Test
	void register_shouldRejectDuplicateUsername() {
		authService.register(new RegisterRequest(
				"dupuser",
				"dup1@example.com",
				"secret123",
				"Dup User",
				""
		));

		assertThrows(BusinessException.class, () -> authService.register(new RegisterRequest(
				"dupuser",
				"dup2@example.com",
				"secret123",
				"Dup User 2",
				""
		)));
	}
}
