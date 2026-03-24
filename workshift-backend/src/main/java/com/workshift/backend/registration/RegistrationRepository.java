package com.workshift.backend.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.User;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
	
	boolean existsByShiftAndUser(Shift shift, User user);

	java.util.Optional<Registration> findByIdAndUser(Long id, User user);
}
