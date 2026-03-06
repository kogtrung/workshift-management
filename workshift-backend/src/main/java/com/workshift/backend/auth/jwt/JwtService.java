package com.workshift.backend.auth.jwt;

import java.time.Instant;
import java.util.Optional;

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
	private final Algorithm algorithm;
	private final JWTVerifier verifier;
	private final String issuer;
	private final long expiresInSeconds;

	public JwtService(
			@Value("${JWT_SECRET:change-me}") String secret,
			@Value("${JWT_ISSUER:workshift-backend}") String issuer,
			@Value("${JWT_EXPIRES_IN_SECONDS:86400}") long expiresInSeconds
	) {
		this.algorithm = Algorithm.HMAC256(secret);
		this.issuer = issuer;
		this.expiresInSeconds = expiresInSeconds;
		this.verifier = JWT.require(this.algorithm).withIssuer(this.issuer).build();
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(expiresInSeconds);

		return JWT.create()
				.withIssuer(issuer)
				.withIssuedAt(now)
				.withExpiresAt(expiresAt)
				.withSubject(String.valueOf(user.getId()))
				.withClaim("username", user.getUsername())
				.withClaim("role", user.getGlobalRole().name())
				.sign(algorithm);
	}

	public Optional<DecodedJWT> verify(String token) {
		try {
			return Optional.of(verifier.verify(token));
		} catch (JWTVerificationException ex) {
			return Optional.empty();
		}
	}
}
