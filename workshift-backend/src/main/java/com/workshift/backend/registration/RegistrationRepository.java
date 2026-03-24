package com.workshift.backend.registration;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.User;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
	
	boolean existsByShiftAndUser(Shift shift, User user);

	java.util.Optional<Registration> findByIdAndUser(Long id, User user);

	@Query("""
			select r from Registration r
			join fetch r.shift s
			join fetch s.group g
			join fetch r.position p
			where r.user.id = :userId
				and r.status = :status
				and s.date between :from and :to
			order by s.date asc, s.startTime asc
			""")
	List<Registration> findByUserIdAndStatusAndShiftDateBetween(
			@Param("userId") Long userId,
			@Param("status") RegistrationStatus status,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to
	);

	long countByShiftIdAndPositionIdAndStatus(Long shiftId, Long positionId, RegistrationStatus status);

	List<Registration> findByShiftIdAndStatus(Long shiftId, RegistrationStatus status);
}
