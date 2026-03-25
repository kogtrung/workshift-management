package com.workshift.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.ShiftRequirement;

@Repository
public interface ShiftRequirementRepository extends JpaRepository<ShiftRequirement, Long> {

	Optional<ShiftRequirement> findByShiftIdAndPositionId(Long shiftId, Long positionId);

	List<ShiftRequirement> findByShiftId(Long shiftId);

	List<ShiftRequirement> findByShiftIdIn(java.util.Collection<Long> shiftIds);

	Optional<ShiftRequirement> findByIdAndShiftId(Long id, Long shiftId);

	void deleteAllByShiftIdIn(Collection<Long> shiftIds);
}

