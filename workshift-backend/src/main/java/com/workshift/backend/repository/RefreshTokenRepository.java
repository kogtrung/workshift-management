package com.workshift.backend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workshift.backend.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);

	int countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(Long userId, Instant now);

	@Modifying
	@Query("update RefreshToken rt set rt.revokedAt = :revokedAt where rt.user.id = :userId and rt.revokedAt is null and rt.expiresAt > :now")
	int revokeAllActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now, @Param("revokedAt") Instant revokedAt);
}
