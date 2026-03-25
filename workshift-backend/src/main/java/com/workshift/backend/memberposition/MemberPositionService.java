package com.workshift.backend.memberposition;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.MemberPosition;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.User;
import com.workshift.backend.memberposition.dto.MemberPositionResponse;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.MemberPositionRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class MemberPositionService {

	private final MemberPositionRepository memberPositionRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final PositionRepository positionRepository;
	private final UserRepository userRepository;

	public MemberPositionService(
			MemberPositionRepository memberPositionRepository,
			GroupMemberRepository groupMemberRepository,
			PositionRepository positionRepository,
			UserRepository userRepository
	) {
		this.memberPositionRepository = memberPositionRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.positionRepository = positionRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<MemberPositionResponse> getMyPositions(String username, Long groupId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		List<MemberPosition> memberPositions = memberPositionRepository
				.findAllByGroupMemberGroupIdAndGroupMemberUserId(groupId, user.getId());

		return memberPositions.stream()
				.map(mp -> new MemberPositionResponse(
						mp.getId(),
						mp.getPosition().getId(),
						mp.getPosition().getName(),
						mp.getPosition().getColorCode()
				))
				.toList();
	}

	@Transactional
	public List<MemberPositionResponse> updateMyPositions(String username, Long groupId, List<Long> positionIds) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc group này"));

		if (membership.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn chưa được duyệt vào group này");
		}

		// Validate all position IDs belong to this group
		List<Position> positions = positionRepository.findByGroupId(groupId);
		List<Long> validPositionIds = positions.stream().map(Position::getId).toList();

		for (Long posId : positionIds) {
			if (!validPositionIds.contains(posId)) {
				throw new BusinessException(HttpStatus.BAD_REQUEST, "Vị trí #" + posId + " không thuộc group này");
			}
		}

		// Remove existing positions
		List<MemberPosition> existing = memberPositionRepository.findAllByGroupMemberId(membership.getId());
		memberPositionRepository.deleteAll(existing);

		// Add new positions
		List<MemberPosition> newMemberPositions = positionIds.stream().map(posId -> {
			Position position = positions.stream()
					.filter(p -> p.getId().equals(posId))
					.findFirst()
					.orElseThrow();

			MemberPosition mp = new MemberPosition();
			mp.setGroupMember(membership);
			mp.setPosition(position);
			return mp;
		}).toList();

		List<MemberPosition> saved = memberPositionRepository.saveAll(newMemberPositions);

		return saved.stream()
				.map(mp -> new MemberPositionResponse(
						mp.getId(),
						mp.getPosition().getId(),
						mp.getPosition().getName(),
						mp.getPosition().getColorCode()
				))
				.toList();
	}
}
