package com.workshift.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.TemplateRequirement;

@Repository
public interface TemplateRequirementRepository extends JpaRepository<TemplateRequirement, Long> {

	List<TemplateRequirement> findByTemplateId(Long templateId);

	Optional<TemplateRequirement> findByTemplateIdAndPositionId(Long templateId, Long positionId);

	void deleteByTemplateId(Long templateId);
}
