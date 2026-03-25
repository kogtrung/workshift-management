package com.workshift.backend.group;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupAuditActionType;
import com.workshift.backend.domain.GroupAuditActorRole;
import com.workshift.backend.domain.GroupAuditEntityType;
import com.workshift.backend.domain.GroupAuditLog;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.User;
import com.workshift.backend.group.dto.GroupAuditDailySummaryResponse;
import com.workshift.backend.group.dto.GroupAuditLogPageResponse;
import com.workshift.backend.group.dto.GroupAuditLogResponse;
import com.workshift.backend.group.dto.GroupAuditMonthlySummaryResponse;
import com.workshift.backend.group.dto.GroupAuditSummaryItem;
import com.workshift.backend.repository.GroupAuditLogRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class GroupAuditService {
	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

	private final GroupAuditLogRepository groupAuditLogRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public GroupAuditService(
			GroupAuditLogRepository groupAuditLogRepository,
			GroupMemberRepository groupMemberRepository,
			UserRepository userRepository,
			ObjectMapper objectMapper
	) {
		this.groupAuditLogRepository = groupAuditLogRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public void recordEvent(
			Group group,
			User actorUser,
			GroupAuditActorRole actorRole,
			GroupAuditActionType actionType,
			GroupAuditEntityType entityType,
			Long entityId,
			String summary,
			Object beforeData,
			Object afterData,
			Map<String, Object> metadata
	) {
		GroupAuditLog log = new GroupAuditLog();
		log.setGroup(group);
		log.setActorUser(actorUser);
		log.setActorRole(actorRole);
		log.setActionType(actionType);
		log.setEntityType(entityType);
		log.setEntityId(entityId);
		log.setOccurredAt(Instant.now());
		log.setSummary(summary);
		log.setBeforeData(toJson(beforeData));
		log.setAfterData(toJson(afterData));
		log.setMetadata(toJson(metadata));
		groupAuditLogRepository.save(log);
	}

	@Transactional(readOnly = true)
	public GroupAuditLogPageResponse getAuditLogs(
			String username,
			Long groupId,
			LocalDate from,
			LocalDate to,
			GroupAuditActionType actionType,
			Long actorUserId,
			GroupAuditEntityType entityType,
			Long entityId,
			int page,
			int size
	) {
		if (page < 0) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "page phải >= 0");
		}
		if (size < 1 || size > 200) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "size phải nằm trong khoảng 1..200");
		}

		User manager = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));
		validateManagerPermission(groupId, manager.getId());

		Specification<GroupAuditLog> specification = Specification.where(byGroupId(groupId));
		if (from != null) {
			specification = specification.and(fromDate(from));
		}
		if (to != null) {
			specification = specification.and(toDate(to));
		}
		if (actionType != null) {
			specification = specification.and(byActionType(actionType));
		}
		if (actorUserId != null) {
			specification = specification.and(byActorUser(actorUserId));
		}
		if (entityType != null) {
			specification = specification.and(byEntityType(entityType));
		}
		if (entityId != null) {
			specification = specification.and(byEntityId(entityId));
		}

		Page<GroupAuditLog> data = groupAuditLogRepository.findAll(
				specification,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"))
		);
		List<GroupAuditLogResponse> items = data.getContent().stream().map(this::toResponse).toList();
		return new GroupAuditLogPageResponse(items, page, size, data.getTotalElements(), data.getTotalPages());
	}

	@Transactional(readOnly = true)
	public GroupAuditDailySummaryResponse getDailySummary(String username, Long groupId, LocalDate date) {
		User manager = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));
		validateManagerPermission(groupId, manager.getId());

		Instant start = date.atStartOfDay(DEFAULT_ZONE).toInstant();
		Instant end = date.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
		List<GroupAuditLog> logs = groupAuditLogRepository.findAll(
				Specification.where(byGroupId(groupId)).and(betweenInstant(start, end))
		);
		return new GroupAuditDailySummaryResponse(groupId, date, logs.size(), summarizeByAction(logs));
	}

	@Transactional(readOnly = true)
	public GroupAuditMonthlySummaryResponse getMonthlySummary(String username, Long groupId, int month, int year) {
		if (month < 1 || month > 12) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Tháng không hợp lệ");
		}
		if (year < 2000 || year > 2100) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Năm không hợp lệ");
		}

		User manager = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng đăng nhập"));
		validateManagerPermission(groupId, manager.getId());

		LocalDate first = LocalDate.of(year, month, 1);
		Instant start = first.atStartOfDay(DEFAULT_ZONE).toInstant();
		Instant end = first.plusMonths(1).atStartOfDay(DEFAULT_ZONE).toInstant();
		List<GroupAuditLog> logs = groupAuditLogRepository.findAll(
				Specification.where(byGroupId(groupId)).and(betweenInstant(start, end))
		);
		return new GroupAuditMonthlySummaryResponse(groupId, month, year, logs.size(), summarizeByAction(logs));
	}

	private List<GroupAuditSummaryItem> summarizeByAction(List<GroupAuditLog> logs) {
		List<GroupAuditSummaryItem> result = new ArrayList<>();
		for (GroupAuditActionType actionType : GroupAuditActionType.values()) {
			long count = logs.stream().filter(log -> log.getActionType() == actionType).count();
			if (count > 0) {
				result.add(new GroupAuditSummaryItem(actionType.name(), count));
			}
		}
		return result;
	}

	private GroupAuditLogResponse toResponse(GroupAuditLog log) {
		User actor = log.getActorUser();
		return new GroupAuditLogResponse(
				log.getId(),
				log.getGroup().getId(),
				actor.getId(),
				actor.getUsername(),
				actor.getFullName(),
				log.getActorRole().name(),
				log.getActionType().name(),
				log.getEntityType().name(),
				log.getEntityId(),
				log.getOccurredAt(),
				log.getSummary(),
				log.getBeforeData(),
				log.getAfterData(),
				log.getMetadata()
		);
	}

	private void validateManagerPermission(Long groupId, Long userId) {
		GroupMember managerMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không thuộc group này"));
		if (managerMembership.getRole() != GroupRole.MANAGER || managerMembership.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem audit của group");
		}
	}

	private String toJson(Object input) {
		if (input == null) {
			return null;
		}
		if (input instanceof String raw) {
			return raw;
		}
		try {
			return objectMapper.writeValueAsString(input);
		} catch (JsonProcessingException e) {
			try {
				return objectMapper.writeValueAsString(String.valueOf(input));
			} catch (JsonProcessingException ignored) {
				return String.valueOf(input);
			}
		}
	}

	private Specification<GroupAuditLog> byGroupId(Long groupId) {
		return (root, query, builder) -> builder.equal(root.get("group").get("id"), groupId);
	}

	private Specification<GroupAuditLog> fromDate(LocalDate from) {
		Instant fromInstant = from.atStartOfDay(DEFAULT_ZONE).toInstant();
		return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("occurredAt"), fromInstant);
	}

	private Specification<GroupAuditLog> toDate(LocalDate to) {
		Instant toExclusive = to.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
		return (root, query, builder) -> builder.lessThan(root.get("occurredAt"), toExclusive);
	}

	private Specification<GroupAuditLog> byActionType(GroupAuditActionType actionType) {
		return (root, query, builder) -> builder.equal(root.get("actionType"), actionType);
	}

	private Specification<GroupAuditLog> byActorUser(Long actorUserId) {
		return (root, query, builder) -> builder.equal(root.get("actorUser").get("id"), actorUserId);
	}

	private Specification<GroupAuditLog> byEntityType(GroupAuditEntityType entityType) {
		return (root, query, builder) -> builder.equal(root.get("entityType"), entityType);
	}

	private Specification<GroupAuditLog> byEntityId(Long entityId) {
		return (root, query, builder) -> builder.equal(root.get("entityId"), entityId);
	}

	private Specification<GroupAuditLog> betweenInstant(Instant start, Instant end) {
		return (root, query, builder) -> builder.and(
				builder.greaterThanOrEqualTo(root.get("occurredAt"), start),
				builder.lessThan(root.get("occurredAt"), end)
		);
	}
}
