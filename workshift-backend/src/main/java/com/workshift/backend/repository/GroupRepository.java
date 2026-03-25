package com.workshift.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workshift.backend.domain.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
	boolean existsByJoinCode(String joinCode);

	Optional<Group> findByJoinCode(String joinCode);

	@org.springframework.data.jpa.repository.Query("SELECT g FROM Group g WHERE " +
			"LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
			"LOWER(g.joinCode) LIKE LOWER(CONCAT('%', :search, '%'))")
	org.springframework.data.domain.Page<Group> searchGroups(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);

	long countByStatus(com.workshift.backend.domain.GroupStatus status);

	@org.springframework.data.jpa.repository.Query("SELECT COUNT(g) FROM Group g WHERE g.createdAt >= :startTime")
	long countCreatedAfter(@org.springframework.data.repository.query.Param("startTime") java.time.Instant startTime);
}
