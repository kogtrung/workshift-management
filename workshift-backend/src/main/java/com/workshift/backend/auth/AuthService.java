package com.workshift.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.auth.dto.RegisterRequest;
import com.workshift.backend.auth.dto.RegisterResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.UserRepository;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new BusinessException(HttpStatus.CONFLICT, "Username đã tồn tại");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(HttpStatus.CONFLICT, "Email đã tồn tại");
		}

		User user = new User();
		user.setUsername(request.username());
		user.setEmail(request.email());
		user.setPassword(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName());
		user.setPhone(request.phone());

		User saved = userRepository.save(user);
		return new RegisterResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getFullName());
	}
}
