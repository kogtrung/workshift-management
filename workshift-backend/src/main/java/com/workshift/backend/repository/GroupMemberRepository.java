package com.workshift.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
	Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

	Optional<GroupMember> findByIdAndGroupId(Long id, Long groupId);

	List<GroupMember> findAllByGroupIdAndStatus(Long groupId, GroupMemberStatus status);

	List<GroupMember> findAllByUserIdAndStatusIn(Long userId, List<GroupMemberStatus> statuses);

	void deleteAllByGroupId(Long groupId);
}
