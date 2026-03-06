package com.workshift.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.auth.dto.LoginRequest;
import com.workshift.backend.auth.dto.LoginResponse;
import com.workshift.backend.auth.dto.RegisterRequest;
import com.workshift.backend.auth.dto.RegisterResponse;
import com.workshift.backend.auth.jwt.JwtService;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.User;
import com.workshift.backend.domain.UserStatus;
import com.workshift.backend.repository.UserRepository;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
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

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByUsername(request.usernameOrEmail())
				.or(() -> userRepository.findByEmail(request.usernameOrEmail()))
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không đúng"));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
		}

		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(user.getUsername(), request.password())
			);
		} catch (AuthenticationException ex) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không đúng");
		}

		String token = jwtService.generateToken(user);
		return new LoginResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getFullName());
	}
}
