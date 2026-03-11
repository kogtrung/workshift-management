package com.workshift.backend.auth.jwt;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.workshift.backend.domain.User;

@Service
public class JwtService {
	private final Algorithm accessAlgorithm;
	private final Algorithm refreshAlgorithm;
	private final JWTVerifier accessVerifier;
	private final JWTVerifier refreshVerifier;
	private final String issuer;
	private final long accessExpiresInSeconds;
	private final long refreshExpiresInSeconds;

	public JwtService(
			@Value("${JWT_SECRET}") String accessSecret,
			@Value("${JWT_REFRESH_SECRET}") String refreshSecret,
			@Value("${JWT_ISSUER:workshift-backend}") String issuer,
			@Value("${JWT_EXPIRES_IN_SECONDS:900}") long accessExpiresInSeconds,
			@Value("${JWT_REFRESH_EXPIRES_IN_SECONDS:604800}") long refreshExpiresInSeconds
	) {
		this.accessAlgorithm = Algorithm.HMAC256(accessSecret);
		this.refreshAlgorithm = Algorithm.HMAC256(refreshSecret);
		this.issuer = issuer;
		this.accessExpiresInSeconds = accessExpiresInSeconds;
		this.refreshExpiresInSeconds = refreshExpiresInSeconds;
		this.accessVerifier = JWT.require(this.accessAlgorithm).withIssuer(this.issuer).withClaim("token_type", "access").build();
		this.refreshVerifier = JWT.require(this.refreshAlgorithm)
				.withIssuer(this.issuer)
				.withClaim("token_type", "refresh")
				.build();
	}

	public String generateAccessToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(accessExpiresInSeconds);

		return JWT.create()
				.withIssuer(issuer)
				.withIssuedAt(now)
				.withExpiresAt(expiresAt)
				.withJWTId(UUID.randomUUID().toString())
				.withSubject(String.valueOf(user.getId()))
				.withClaim("username", user.getUsername())
				.withClaim("role", user.getGlobalRole().name())
				.withClaim("token_type", "access")
				.sign(accessAlgorithm);
	}

	public String generateRefreshToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(refreshExpiresInSeconds);

		return JWT.create()
				.withIssuer(issuer)
				.withIssuedAt(now)
				.withExpiresAt(expiresAt)
				.withJWTId(UUID.randomUUID().toString())
				.withSubject(String.valueOf(user.getId()))
				.withClaim("username", user.getUsername())
				.withClaim("token_type", "refresh")
				.sign(refreshAlgorithm);
	}

	public Optional<DecodedJWT> verifyAccessToken(String token) {
		try {
			return Optional.of(accessVerifier.verify(token));
		} catch (JWTVerificationException ex) {
			return Optional.empty();
		}
	}

	public Optional<DecodedJWT> verifyRefreshToken(String token) {
		try {
			return Optional.of(refreshVerifier.verify(token));
		} catch (JWTVerificationException ex) {
			return Optional.empty();
		}
	}

	public Instant getRefreshTokenExpiresAt(DecodedJWT decodedJWT) {
		return decodedJWT.getExpiresAt().toInstant();
	}
}
