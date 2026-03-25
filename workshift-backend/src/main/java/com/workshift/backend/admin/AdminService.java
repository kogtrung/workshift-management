package com.workshift.backend.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.admin.dto.AdminUserResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GlobalRole;
import com.workshift.backend.domain.User;
import com.workshift.backend.domain.UserStatus;
import com.workshift.backend.repository.UserRepository;

import com.workshift.backend.admin.dto.AdminGroupResponse;
import com.workshift.backend.domain.AdminAuditLog;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupStatus;
import com.workshift.backend.repository.AdminAuditLogRepository;
import com.workshift.backend.repository.GroupRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import com.workshift.backend.admin.dto.AdminAuditLogResponse;

@Service
public class AdminService {

	private final UserRepository userRepository;
	private final GroupRepository groupRepository;
	private final AdminAuditLogRepository auditLogRepository;

	public AdminService(UserRepository userRepository, GroupRepository groupRepository, AdminAuditLogRepository auditLogRepository) {
		this.userRepository = userRepository;
		this.groupRepository = groupRepository;
		this.auditLogRepository = auditLogRepository;
	}

	private void logAdminAction(String action, String target, String detail) {
		String adminUsername = "system";
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
			adminUsername = auth.getName();
		}
		AdminAuditLog log = new AdminAuditLog();
		log.setAdminUsername(adminUsername);
		log.setAction(action);
		log.setTarget(target);
		log.setDetail(detail);
		auditLogRepository.save(log);
	}

	@Transactional(readOnly = true)
	public Page<AdminGroupResponse> getGroups(String search, Pageable pageable) {
		Page<Group> groupsPage;
		if (search == null || search.trim().isEmpty()) {
			groupsPage = groupRepository.findAll(pageable);
		} else {
			groupsPage = groupRepository.searchGroups(search.trim(), pageable);
		}

		return groupsPage.map(g -> new AdminGroupResponse(
				g.getId(), g.getName(), g.getJoinCode(), g.getDescription(),
				g.getCreatedBy().getId(), g.getCreatedBy().getUsername(),
				g.getStatus(), g.getCreatedAt()
		));
	}

	@Transactional
	public AdminGroupResponse toggleGroupStatus(Long groupId) {
		Group group = groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Nhóm không tồn tại"));

		if (group.getStatus() == GroupStatus.ACTIVE) {
			group.setStatus(GroupStatus.INACTIVE);
		} else {
			group.setStatus(GroupStatus.ACTIVE);
		}
		
		group = groupRepository.save(group);

		logAdminAction("TOGGLE_GROUP_STATUS", "GROUP:" + group.getId(), "Status changed to " + group.getStatus());

		return new AdminGroupResponse(
				group.getId(), group.getName(), group.getJoinCode(), group.getDescription(),
				group.getCreatedBy().getId(), group.getCreatedBy().getUsername(),
				group.getStatus(), group.getCreatedAt()
		);
	}

	@Transactional(readOnly = true)
	public Page<AdminUserResponse> getUsers(String search, Pageable pageable) {
		Page<User> usersPage;
		if (search == null || search.trim().isEmpty()) {
			usersPage = userRepository.findAll(pageable);
		} else {
			usersPage = userRepository.searchUsers(search.trim(), pageable);
		}

		return usersPage.map(u -> new AdminUserResponse(
				u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
				u.getPhone(), u.getStatus(), u.getGlobalRole(), u.getCreatedAt()
		));
	}

	@Transactional
	public AdminUserResponse toggleUserStatus(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User không tồn tại"));

		if (user.getGlobalRole() == GlobalRole.ADMIN) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Không thể thay đổi trạng thái của tài khoản ADMIN!");
		}

		if (user.getStatus() == UserStatus.ACTIVE) {
			user.setStatus(UserStatus.BANNED);
		} else {
			user.setStatus(UserStatus.ACTIVE);
		}
		
		user = userRepository.save(user);

		logAdminAction("TOGGLE_USER_STATUS", "USER:" + user.getId(), "Status changed to " + user.getStatus());

		return new AdminUserResponse(
				user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
				user.getPhone(), user.getStatus(), user.getGlobalRole(), user.getCreatedAt()
		);
	}

	@Transactional(readOnly = true)
	public com.workshift.backend.admin.dto.SystemMetricsResponse getSystemMetrics() {
		long totalUsers = userRepository.count();
		long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
		long bannedUsers = userRepository.countByStatus(UserStatus.BANNED);

		long totalGroups = groupRepository.count();
		long activeGroups = groupRepository.countByStatus(com.workshift.backend.domain.GroupStatus.ACTIVE);
		long inactiveGroups = groupRepository.countByStatus(com.workshift.backend.domain.GroupStatus.INACTIVE);

		java.time.Instant now = java.time.Instant.now();
		java.time.ZonedDateTime zdt = now.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
		java.time.Instant startOfDay = zdt.toLocalDate().atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
		java.time.Instant startOfMonth = zdt.toLocalDate().withDayOfMonth(1).atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();

		long newUsersToday = userRepository.countCreatedAfter(startOfDay);
		long newUsersThisMonth = userRepository.countCreatedAfter(startOfMonth);
		long newGroupsToday = groupRepository.countCreatedAfter(startOfDay);
		long newGroupsThisMonth = groupRepository.countCreatedAfter(startOfMonth);

		long failedLogins = 0;
		long activeWarnings = 0;

		return new com.workshift.backend.admin.dto.SystemMetricsResponse(
				totalUsers, activeUsers, bannedUsers, newUsersToday,
				totalGroups, activeGroups, inactiveGroups, newGroupsToday,
				newUsersThisMonth, newGroupsThisMonth, failedLogins, activeWarnings
		);
	}

	@Transactional(readOnly = true)
	public Page<AdminAuditLogResponse> getAuditLogs(Pageable pageable) {
		return auditLogRepository.findAll(pageable).map(log -> new AdminAuditLogResponse(
				log.getId(), log.getAdminUsername(), log.getAction(),
				log.getTarget(), log.getDetail(), log.getCreatedAt()
		));
	}
}
