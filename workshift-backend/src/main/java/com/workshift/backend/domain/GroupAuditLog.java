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

@Entity
@Table(name = "group_audit_logs")
public class GroupAuditLog extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private Group group;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_user_id", nullable = false)
	private User actorUser;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_role", nullable = false, length = 20)
	private GroupAuditActorRole actorRole;

	@Enumerated(EnumType.STRING)
	@Column(name = "action_type", nullable = false, length = 50)
	private GroupAuditActionType actionType;

	@Enumerated(EnumType.STRING)
	@Column(name = "entity_type", nullable = false, length = 30)
	private GroupAuditEntityType entityType;

	@Column(name = "entity_id", nullable = false)
	private Long entityId;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "summary", nullable = false, length = 500)
	private String summary;

	@Column(name = "before_data", columnDefinition = "TEXT")
	private String beforeData;

	@Column(name = "after_data", columnDefinition = "TEXT")
	private String afterData;

	@Column(name = "metadata", columnDefinition = "TEXT")
	private String metadata;

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public User getActorUser() {
		return actorUser;
	}

	public void setActorUser(User actorUser) {
		this.actorUser = actorUser;
	}

	public GroupAuditActorRole getActorRole() {
		return actorRole;
	}

	public void setActorRole(GroupAuditActorRole actorRole) {
		this.actorRole = actorRole;
	}

	public GroupAuditActionType getActionType() {
		return actionType;
	}

	public void setActionType(GroupAuditActionType actionType) {
		this.actionType = actionType;
	}

	public GroupAuditEntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(GroupAuditEntityType entityType) {
		this.entityType = entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getBeforeData() {
		return beforeData;
	}

	public void setBeforeData(String beforeData) {
		this.beforeData = beforeData;
	}

	public String getAfterData() {
		return afterData;
	}

	public void setAfterData(String afterData) {
		this.afterData = afterData;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
}
