package com.workshift.backend.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.Shift;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

	@Query("SELECT s FROM Shift s WHERE s.group.id = :groupId AND s.date = :date AND " +
		   "((s.startTime < :endTime AND s.endTime > :startTime))")
	List<Shift> findOverlappingShifts(@Param("groupId") Long groupId,
									 @Param("date") LocalDate date,
									 @Param("startTime") LocalTime startTime,
									 @Param("endTime") LocalTime endTime);
}
