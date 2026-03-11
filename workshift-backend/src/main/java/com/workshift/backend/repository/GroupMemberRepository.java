package com.workshift.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workshift.backend.domain.GroupMember;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
	Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
}
