package com.workshift.backend.shift;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shift.dto.CreateShiftRequest;
import com.workshift.backend.shift.dto.CreateShiftResponse;

@Service
public class ShiftService {

	private final ShiftRepository shiftRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final UserRepository userRepository;

	public ShiftService(ShiftRepository shiftRepository, 
						GroupRepository groupRepository,
						GroupMemberRepository groupMemberRepository,
						UserRepository userRepository) {
		this.shiftRepository = shiftRepository;
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public CreateShiftResponse createShift(Long groupId, String username, CreateShiftRequest request) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		Group group = groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

		// Kiểm tra quyền MANAGER
		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền tạo ca làm việc");
		}

		// Validate thời gian
		if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Giờ bắt đầu phải trước giờ kết thúc");
		}

		// Kiểm tra trùng lặp (Overlap)
		List<Shift> overlappingShifts = shiftRepository.findOverlappingShifts(
				groupId, 
				request.getDate(), 
				request.getStartTime(), 
				request.getEndTime()
		);

		if (!overlappingShifts.isEmpty()) {
			throw new BusinessException(HttpStatus.CONFLICT, "Ca làm việc bị trùng lặp thời gian với ca khác trong cùng ngày");
		}

		Shift shift = new Shift();
		shift.setGroup(group);
		shift.setName(request.getName());
		shift.setDate(request.getDate());
		shift.setStartTime(request.getStartTime());
		shift.setEndTime(request.getEndTime());
		shift.setStatus(ShiftStatus.OPEN);

		Shift savedShift = shiftRepository.save(shift);

		CreateShiftResponse response = new CreateShiftResponse();
		response.setId(savedShift.getId());
		response.setName(savedShift.getName());
		response.setDate(savedShift.getDate());
		response.setStartTime(savedShift.getStartTime());
		response.setEndTime(savedShift.getEndTime());
		response.setStatus(savedShift.getStatus());

		return response;
	}
}
