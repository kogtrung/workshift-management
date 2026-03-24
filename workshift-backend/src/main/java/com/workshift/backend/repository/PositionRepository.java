package com.workshift.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.Position;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

	List<Position> findByGroupId(Long groupId);

	Optional<Position> findByIdAndGroupId(Long id, Long groupId);

	boolean existsByGroupIdAndName(Long groupId, String name);

	boolean existsByGroupIdAndNameAndIdNot(Long groupId, String name, Long id);
}
