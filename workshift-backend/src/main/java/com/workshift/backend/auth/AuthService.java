package com.workshift.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.auth.dto.LoginRequest;
import com.workshift.backend.auth.dto.LoginResponse;
import com.workshift.backend.auth.dto.RefreshTokenResponse;
import com.workshift.backend.auth.dto.RegisterRequest;
import com.workshift.backend.auth.dto.RegisterResponse;
import com.workshift.backend.auth.jwt.JwtService;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.RefreshToken;
import com.workshift.backend.domain.User;
import com.workshift.backend.domain.UserStatus;
import com.workshift.backend.repository.RefreshTokenRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
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

	@Transactional
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

		String accessToken = jwtService.generateAccessToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);
		saveRefreshToken(user, refreshToken);
		return new LoginResponse(accessToken, refreshToken, user.getId(), user.getUsername(), user.getEmail(), user.getFullName());
	}

	@Transactional
	public RefreshTokenResponse refresh(String rawRefreshToken) {
		jwtService.verifyRefreshToken(rawRefreshToken)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ"));

		RefreshToken refreshToken = refreshTokenRepository
				.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hashToken(rawRefreshToken), Instant.now())
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn hoặc bị thu hồi"));

		User user = refreshToken.getUser();
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
		}

		refreshToken.setRevokedAt(Instant.now());

		String newAccessToken = jwtService.generateAccessToken(user);
		String newRefreshToken = jwtService.generateRefreshToken(user);
		saveRefreshToken(user, newRefreshToken);
		return new RefreshTokenResponse(newAccessToken, newRefreshToken);
	}

	@Transactional
	public void logout(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));

		Instant now = Instant.now();
		refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now, now);
	}

	private void saveRefreshToken(User user, String rawRefreshToken) {
		var decoded = jwtService.verifyRefreshToken(rawRefreshToken)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ"));

		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setTokenHash(hashToken(rawRefreshToken));
		refreshToken.setExpiresAt(jwtService.getRefreshTokenExpiresAt(decoded));
		refreshTokenRepository.save(refreshToken);
	}

	private String hashToken(String rawToken) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] hashedBytes = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hashedBytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
