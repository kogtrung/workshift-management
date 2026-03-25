package com.workshift.backend.domain;

import java.time.Instant;

import com.workshift.backend.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "group_members",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_group_member_user_group", columnNames = {"group_id", "user_id"})
		}
)
public class GroupMember extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private Group group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private GroupRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private GroupMemberStatus status;

	@Column(name = "joined_at")
	private Instant joinedAt;

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public GroupRole getRole() {
		return role;
	}

	public void setRole(GroupRole role) {
		this.role = role;
	}

	public GroupMemberStatus getStatus() {
		return status;
	}

	public void setStatus(GroupMemberStatus status) {
		this.status = status;
	}

	public Instant getJoinedAt() {
		return joinedAt;
	}

	public void setJoinedAt(Instant joinedAt) {
		this.joinedAt = joinedAt;
	}
}
