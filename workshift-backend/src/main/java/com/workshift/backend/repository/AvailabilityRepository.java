package com.workshift.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.Availability;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

	List<Availability> findByUserId(Long userId);

	@Modifying
	@Query("DELETE FROM Availability a WHERE a.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);
}
