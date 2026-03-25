package com.workshift.backend.shiftchange;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workshift.backend.domain.ShiftChangeRequest;
import com.workshift.backend.domain.ShiftChangeRequestStatus;
import com.workshift.backend.domain.User;

@Repository
public interface ShiftChangeRequestRepository extends JpaRepository<ShiftChangeRequest, Long> {

    Optional<ShiftChangeRequest> findByIdAndRequester(Long id, User requester);

    List<ShiftChangeRequest> findByToShift_Group_IdAndStatus(Long groupId, ShiftChangeRequestStatus status);

    List<ShiftChangeRequest> findByRequesterIdAndStatus(Long requesterId, ShiftChangeRequestStatus status);
}
