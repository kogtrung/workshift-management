package com.workshift.backend.admin;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.workshift.backend.admin.dto.AdminUserResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GlobalRole;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.domain.UserStatus;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private GroupRepository groupRepository;

	@Mock
	private com.workshift.backend.repository.AdminAuditLogRepository auditLogRepository;

	@InjectMocks
	private AdminService adminService;

	private User mockUser;
	private User mockAdmin;
	private Group mockGroup;

	@BeforeEach
	void setUp() {
		mockUser = new User();
		mockUser.setId(1L);
		mockUser.setUsername("testuser");
		mockUser.setEmail("test@ex.com");
		mockUser.setFullName("Test User");
		mockUser.setStatus(UserStatus.ACTIVE);
		mockUser.setGlobalRole(GlobalRole.USER);

		mockAdmin = new User();
		mockAdmin.setId(2L);
		mockAdmin.setUsername("admin");
		mockAdmin.setStatus(UserStatus.ACTIVE);
		mockAdmin.setGlobalRole(GlobalRole.ADMIN);

		mockGroup = new Group();
		mockGroup.setId(10L);
		mockGroup.setName("Test Group");
		mockGroup.setJoinCode("123456");
		mockGroup.setStatus(GroupStatus.ACTIVE);
		mockGroup.setCreatedBy(mockUser);
	}

	@Test
	void getUsers_NoSearch_ReturnsPage() {
		Pageable pageable = PageRequest.of(0, 10);
		Page<User> page = new PageImpl<>(List.of(mockUser));
		when(userRepository.findAll(pageable)).thenReturn(page);

		Page<AdminUserResponse> result = adminService.getUsers(null, pageable);
		
		assertEquals(1, result.getTotalElements());
		assertEquals("testuser", result.getContent().get(0).username());
	}

	@Test
	void getUsers_WithSearch_ReturnsPage() {
		Pageable pageable = PageRequest.of(0, 10);
		Page<User> page = new PageImpl<>(List.of(mockUser));
		when(userRepository.searchUsers("test", pageable)).thenReturn(page);

		Page<AdminUserResponse> result = adminService.getUsers("test", pageable);
		
		assertEquals(1, result.getTotalElements());
	}

	@Test
	void toggleUserStatus_ActiveToBanned_Success() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		AdminUserResponse res = adminService.toggleUserStatus(1L);

		assertEquals(UserStatus.BANNED, res.status());
		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	void toggleUserStatus_Admin_ThrowsException() {
		when(userRepository.findById(2L)).thenReturn(Optional.of(mockAdmin));

		BusinessException ex = assertThrows(BusinessException.class, () -> adminService.toggleUserStatus(2L));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
		assertTrue(ex.getMessage().contains("ADMIN"));
	}

	@Test
	void getGroups_NoSearch_ReturnsPage() {
		Pageable pageable = PageRequest.of(0, 10);
		Page<Group> page = new PageImpl<>(List.of(mockGroup));
		when(groupRepository.findAll(pageable)).thenReturn(page);

		Page<com.workshift.backend.admin.dto.AdminGroupResponse> result = adminService.getGroups(null, pageable);
		
		assertEquals(1, result.getTotalElements());
		assertEquals("Test Group", result.getContent().get(0).name());
	}

	@Test
	void toggleGroupStatus_Success() {
		when(groupRepository.findById(10L)).thenReturn(Optional.of(mockGroup));
		when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);

		com.workshift.backend.admin.dto.AdminGroupResponse res = adminService.toggleGroupStatus(10L);

		assertEquals(GroupStatus.INACTIVE, res.status());
		verify(groupRepository, times(1)).save(any(Group.class));
	}

	@Test
	void getSystemMetrics_ReturnsMetrics() {
		when(userRepository.count()).thenReturn(10L);
		when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(8L);
		when(userRepository.countByStatus(UserStatus.BANNED)).thenReturn(2L);

		when(groupRepository.count()).thenReturn(5L);
		when(groupRepository.countByStatus(GroupStatus.ACTIVE)).thenReturn(4L);
		when(groupRepository.countByStatus(GroupStatus.INACTIVE)).thenReturn(1L);

		when(userRepository.countCreatedAfter(any(java.time.Instant.class))).thenReturn(3L);
		when(groupRepository.countCreatedAfter(any(java.time.Instant.class))).thenReturn(1L);

		com.workshift.backend.admin.dto.SystemMetricsResponse res = adminService.getSystemMetrics();

		assertEquals(10L, res.totalUsers());
		assertEquals(8L, res.activeUsers());
		assertEquals(5L, res.totalGroups());
		assertEquals(1L, res.newGroupsToday());
		assertEquals(3L, res.newUsersThisMonth());
	}
}
