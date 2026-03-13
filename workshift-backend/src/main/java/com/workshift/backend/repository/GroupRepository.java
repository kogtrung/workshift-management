package com.workshift.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workshift.backend.domain.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
	boolean existsByJoinCode(String joinCode);

	Optional<Group> findByJoinCode(String joinCode);
}
