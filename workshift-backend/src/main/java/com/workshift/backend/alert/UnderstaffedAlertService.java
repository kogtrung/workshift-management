package com.workshift.backend.alert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.alert.dto.UnderstaffedAlertResponse;
import com.workshift.backend.alert.dto.UnderstaffedAlertResponse.PositionShortage;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftRequirement;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.ShiftRequirementRepository;
import com.workshift.backend.repository.UserRepository;

@Service
public class UnderstaffedAlertService {

	private final ShiftRepository shiftRepository;
	private final ShiftRequirementRepository shiftRequirementRepository;
	private final RegistrationRepository registrationRepository;
	private final UserRepository userRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;

	public UnderstaffedAlertService(ShiftRepository shiftRepository,
									ShiftRequirementRepository shiftRequirementRepository,
									RegistrationRepository registrationRepository,
									UserRepository userRepository,
									GroupRepository groupRepository,
									GroupMemberRepository groupMemberRepository) {
		this.shiftRepository = shiftRepository;
		this.shiftRequirementRepository = shiftRequirementRepository;
		this.registrationRepository = registrationRepository;
		this.userRepository = userRepository;
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
	}

	@Transactional(readOnly = true)
	public List<UnderstaffedAlertResponse> getUnderstaffedShifts(Long groupId, String username) {
		// 1. Xác thực user
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		// 2. Kiểm tra group tồn tại
		groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

		// 3. Kiểm tra quyền MANAGER
		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền xem cảnh báo thiếu người");
		}

		// 4. Lấy ca OPEN từ hôm nay trở đi
		List<Shift> upcomingShifts = shiftRepository
				.findByGroupIdAndStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
						groupId, ShiftStatus.OPEN, LocalDate.now());

		if (upcomingShifts.isEmpty()) {
			return List.of();
		}

		// 5. Batch load ShiftRequirements
		List<Long> shiftIds = upcomingShifts.stream().map(Shift::getId).toList();

		Map<Long, List<ShiftRequirement>> reqMap = shiftRequirementRepository.findByShiftIdIn(shiftIds)
				.stream()
				.collect(Collectors.groupingBy(r -> r.getShift().getId()));

		// 6. Batch đếm APPROVED registrations theo (shiftId, positionId)
		Map<String, Long> approvedMap = new HashMap<>();
		List<Object[]> approvedCounts = registrationRepository.countGroupedByShiftAndPosition(
				shiftIds, RegistrationStatus.APPROVED);
		for (Object[] row : approvedCounts) {
			Long sId = (Long) row[0];
			Long pId = (Long) row[1];
			Long count = (Long) row[2];
			approvedMap.put(sId + "_" + pId, count);
		}

		// 7. So sánh và tạo response
		List<UnderstaffedAlertResponse> result = new ArrayList<>();

		for (Shift shift : upcomingShifts) {
			List<ShiftRequirement> requirements = reqMap.getOrDefault(shift.getId(), List.of());
			if (requirements.isEmpty()) {
				continue;
			}

			List<PositionShortage> shortages = new ArrayList<>();
			int totalRequired = 0;
			int totalApproved = 0;

			for (ShiftRequirement req : requirements) {
				int required = req.getQuantity();
				long approved = approvedMap.getOrDefault(shift.getId() + "_" + req.getPosition().getId(), 0L);

				totalRequired += required;
				totalApproved += (int) approved;

				if (approved < required) {
					shortages.add(new PositionShortage(
							req.getPosition().getId(),
							req.getPosition().getName(),
							required,
							(int) approved,
							required - (int) approved
					));
				}
			}

			if (!shortages.isEmpty()) {
				UnderstaffedAlertResponse response = new UnderstaffedAlertResponse();
				response.setShiftId(shift.getId());
				response.setShiftName(shift.getName());
				response.setDate(shift.getDate());
				response.setStartTime(shift.getStartTime());
				response.setEndTime(shift.getEndTime());
				response.setShortages(shortages);
				response.setTotalRequired(totalRequired);
				response.setTotalApproved(totalApproved);
				result.add(response);
			}
		}

		return result;
	}
}
