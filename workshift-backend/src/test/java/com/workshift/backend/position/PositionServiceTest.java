package com.workshift.backend.position;

import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.User;
import com.workshift.backend.position.dto.CreatePositionRequest;
import com.workshift.backend.position.dto.PositionResponse;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

	@Mock
	private PositionRepository positionRepository;
	@Mock
	private GroupRepository groupRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private GroupMemberRepository groupMemberRepository;

	@InjectMocks
	private PositionService positionService;

	private User mockUser;
	private Group mockGroup;
	private GroupMember mockMembership;

	@BeforeEach
	void setUp() {
		mockUser = new User();
		mockUser.setId(1L);
		mockUser.setUsername("manager_user");

		mockGroup = new Group();
		mockGroup.setId(10L);
		mockGroup.setName("Test Group");

		mockMembership = new GroupMember();
		mockMembership.setUser(mockUser);
		mockMembership.setGroup(mockGroup);
		mockMembership.setRole(GroupRole.MANAGER);
		mockMembership.setStatus(GroupMemberStatus.APPROVED);
	}

	@Test
	void createPosition_Success() {
		CreatePositionRequest req = new CreatePositionRequest("Pha chế", "#FF0000");

		when(userRepository.findByUsername("manager_user")).thenReturn(Optional.of(mockUser));
		when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(mockMembership));
		when(groupRepository.findById(10L)).thenReturn(Optional.of(mockGroup));
		when(positionRepository.existsByGroupIdAndName(10L, "Pha chế")).thenReturn(false);

		Position savedPos = new Position();
		savedPos.setId(100L);
		savedPos.setGroup(mockGroup);
		savedPos.setName("Pha chế");
		savedPos.setColorCode("#FF0000");

		when(positionRepository.save(any(Position.class))).thenReturn(savedPos);

		PositionResponse res = positionService.createPosition("manager_user", 10L, req);

		assertThat(res.id()).isEqualTo(100L);
		assertThat(res.name()).isEqualTo("Pha chế");
		verify(positionRepository, times(1)).save(any(Position.class));
	}

	@Test
	void createPosition_FailsWhenNotManager() {
		mockMembership.setRole(GroupRole.MEMBER);
		CreatePositionRequest req = new CreatePositionRequest("Pha chế", "#FF0000");

		when(userRepository.findByUsername("manager_user")).thenReturn(Optional.of(mockUser));
		when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(mockMembership));

		assertThatThrownBy(() -> positionService.createPosition("manager_user", 10L, req))
				.isInstanceOf(BusinessException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void getPositions_SuccessForApprovedMember() {
		mockMembership.setRole(GroupRole.MEMBER);
		when(userRepository.findByUsername("manager_user")).thenReturn(Optional.of(mockUser));
		when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(mockMembership));

		Position p1 = new Position();
		p1.setId(100L);
		p1.setName("Pos 1");
		when(positionRepository.findByGroupId(10L)).thenReturn(List.of(p1));

		List<PositionResponse> res = positionService.getPositions("manager_user", 10L);
		assertThat(res).hasSize(1);
		assertThat(res.get(0).name()).isEqualTo("Pos 1");
	}
}
