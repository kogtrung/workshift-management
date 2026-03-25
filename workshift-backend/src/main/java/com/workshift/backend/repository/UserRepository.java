package com.workshift.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workshift.backend.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	@org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
			"LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
	org.springframework.data.domain.Page<User> searchUsers(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);

	long countByStatus(com.workshift.backend.domain.UserStatus status);

	@org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startOfDay")
	long countCreatedAfter(@org.springframework.data.repository.query.Param("startOfDay") java.time.Instant startOfDay);
}
