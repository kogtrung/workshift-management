package com.workshift.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workshift.backend.domain.MemberPosition;

public interface MemberPositionRepository extends JpaRepository<MemberPosition, Long> {
	List<MemberPosition> findAllByGroupMemberId(Long groupMemberId);

	List<MemberPosition> findAllByGroupMemberGroupIdAndGroupMemberUserId(Long groupId, Long userId);

	boolean existsByGroupMemberIdAndPositionId(Long groupMemberId, Long positionId);

	void deleteByGroupMemberIdAndPositionId(Long groupMemberId, Long positionId);

	void deleteAllByGroupMemberId(Long groupMemberId);

	void deleteAllByGroupMemberGroupId(Long groupId);
}
