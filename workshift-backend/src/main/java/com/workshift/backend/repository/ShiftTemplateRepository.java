package com.workshift.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.ShiftTemplate;

@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {
	
	List<ShiftTemplate> findByGroupId(Long groupId);
	
	Optional<ShiftTemplate> findByIdAndGroupId(Long id, Long groupId);
	
	boolean existsByGroupIdAndName(Long groupId, String name);

	boolean existsByGroupIdAndNameAndIdNot(Long groupId, String name, Long id);
}
