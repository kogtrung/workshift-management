package com.workshift.backend.admin.dto;

public record SystemMetricsResponse(
		long totalUsers,
		long activeUsers,
		long bannedUsers,
		long newUsersToday,
		long totalGroups,
		long activeGroups,
		long inactiveGroups,
		long newGroupsToday,
		long newUsersThisMonth,
		long newGroupsThisMonth,
		long failedLogins,
		long activeWarnings
) {}
