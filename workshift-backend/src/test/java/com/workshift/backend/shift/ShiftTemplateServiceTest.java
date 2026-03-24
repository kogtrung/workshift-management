package com.workshift.backend.shift;

import java.time.LocalTime;
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
import com.workshift.backend.domain.ShiftTemplate;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.ShiftTemplateRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shift.dto.template.CreateShiftTemplateRequest;
import com.workshift.backend.shift.dto.template.ShiftTemplateResponse;

@ExtendWith(MockitoExtension.class)
class ShiftTemplateServiceTest {

	@Mock
	private ShiftTemplateRepository shiftTemplateRepository;
	@Mock
	private GroupRepository groupRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private GroupMemberRepository groupMemberRepository;

	@InjectMocks
	private ShiftTemplateService shiftTemplateService;

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
	void createTemplate_Success() {
		CreateShiftTemplateRequest req = new CreateShiftTemplateRequest("Ca Sáng", LocalTime.of(8, 0), LocalTime.of(12, 0), "Mô tả");

		when(userRepository.findByUsername("manager_user")).thenReturn(Optional.of(mockUser));
		when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(mockMembership));
		when(groupRepository.findById(10L)).thenReturn(Optional.of(mockGroup));
		when(shiftTemplateRepository.existsByGroupIdAndName(10L, "Ca Sáng")).thenReturn(false);

		ShiftTemplate savedTemplate = new ShiftTemplate();
		savedTemplate.setId(100L);
		savedTemplate.setGroup(mockGroup);
		savedTemplate.setName("Ca Sáng");
		savedTemplate.setStartTime(LocalTime.of(8, 0));
		savedTemplate.setEndTime(LocalTime.of(12, 0));

		when(shiftTemplateRepository.save(any(ShiftTemplate.class))).thenReturn(savedTemplate);

		ShiftTemplateResponse res = shiftTemplateService.createTemplate("manager_user", 10L, req);

		assertThat(res.id()).isEqualTo(100L);
		assertThat(res.name()).isEqualTo("Ca Sáng");
		verify(shiftTemplateRepository, times(1)).save(any(ShiftTemplate.class));
	}

	@Test
	void createTemplate_FailsWhenNotManager() {
		mockMembership.setRole(GroupRole.MEMBER);
		CreateShiftTemplateRequest req = new CreateShiftTemplateRequest("Ca Sáng", LocalTime.of(8, 0), LocalTime.of(12, 0), "Mô tả");

		when(userRepository.findByUsername("manager_user")).thenReturn(Optional.of(mockUser));
		when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(mockMembership));

		assertThatThrownBy(() -> shiftTemplateService.createTemplate("manager_user", 10L, req))
				.isInstanceOf(BusinessException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void getTemplates_SuccessForApprovedMember() {
		mockMembership.setRole(GroupRole.MEMBER);
		when(userRepository.findByUsername("manager_user")).thenReturn(Optional.of(mockUser));
		when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(mockMembership));

		ShiftTemplate t1 = new ShiftTemplate();
		t1.setId(100L);
		t1.setName("Ca Sáng");
		t1.setGroup(mockGroup);
		
		when(shiftTemplateRepository.findByGroupId(10L)).thenReturn(List.of(t1));

		List<ShiftTemplateResponse> res = shiftTemplateService.getTemplates("manager_user", 10L);
		assertThat(res).hasSize(1);
		assertThat(res.get(0).name()).isEqualTo("Ca Sáng");
	}
}
