package com.workshift.backend.shift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shift.dto.CreateShiftRequest;
import com.workshift.backend.shift.dto.CreateShiftResponse;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShiftServiceTest {

	@Autowired
	private ShiftService shiftService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private ShiftRepository shiftRepository;

	private User manager;
	private Group group;

	@BeforeEach
	void setUp() {
		manager = new User();
		manager.setUsername("manager_test");
		manager.setEmail("manager@test.com");
		manager.setPassword("password");
		manager.setFullName("Manager Test");
		manager = userRepository.save(manager);

		group = new Group();
		group.setName("Test Group");
		group.setCreatedBy(manager);
		group = groupRepository.save(group);

		GroupMember groupMember = new GroupMember();
		groupMember.setGroup(group);
		groupMember.setUser(manager);
		groupMember.setRole(GroupRole.MANAGER);
		groupMember.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(groupMember);
	}

	@Test
	void createShift_success() {
		CreateShiftRequest request = new CreateShiftRequest();
		request.setName("Ca sáng");
		request.setDate(LocalDate.now().plusDays(1));
		request.setStartTime(LocalTime.of(8, 0));
		request.setEndTime(LocalTime.of(12, 0));

		CreateShiftResponse response = shiftService.createShift(group.getId(), manager.getUsername(), request);

		assertNotNull(response.getId());
		assertEquals("Ca sáng", response.getName());
		assertEquals(ShiftStatus.OPEN, response.getStatus());
	}

	@Test
	void createShift_fail_startTimeAfterEndTime() {
		CreateShiftRequest request = new CreateShiftRequest();
		request.setName("Ca lỗi");
		request.setDate(LocalDate.now().plusDays(1));
		request.setStartTime(LocalTime.of(12, 0));
		request.setEndTime(LocalTime.of(8, 0));

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			shiftService.createShift(group.getId(), manager.getUsername(), request);
		});

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Giờ bắt đầu phải trước giờ kết thúc", exception.getMessage());
	}

	@Test
	void createShift_fail_overlap() {
		// Tạo ca đầu tiên: 08:00 - 12:00
		CreateShiftRequest request1 = new CreateShiftRequest();
		request1.setName("Ca 1");
		request1.setDate(LocalDate.now().plusDays(1));
		request1.setStartTime(LocalTime.of(8, 0));
		request1.setEndTime(LocalTime.of(12, 0));
		shiftService.createShift(group.getId(), manager.getUsername(), request1);

		// Thử tạo ca thứ hai bị trùng: 10:00 - 14:00 (Overlap)
		CreateShiftRequest request2 = new CreateShiftRequest();
		request2.setName("Ca 2");
		request2.setDate(LocalDate.now().plusDays(1));
		request2.setStartTime(LocalTime.of(10, 0));
		request2.setEndTime(LocalTime.of(14, 0));

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			shiftService.createShift(group.getId(), manager.getUsername(), request2);
		});

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		assertEquals("Ca làm việc bị trùng lặp thời gian với ca khác trong cùng ngày", exception.getMessage());
	}

	@Test
	void createShift_fail_notManager() {
		User member = new User();
		member.setUsername("member_test");
		member.setEmail("member@test.com");
		member.setPassword("password");
		member.setFullName("Member Test");
		member = userRepository.save(member);

		GroupMember groupMember = new GroupMember();
		groupMember.setGroup(group);
		groupMember.setUser(member);
		groupMember.setRole(GroupRole.MEMBER);
		groupMember.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(groupMember);

		CreateShiftRequest request = new CreateShiftRequest();
		request.setName("Ca Member");
		request.setDate(LocalDate.now().plusDays(1));
		request.setStartTime(LocalTime.of(8, 0));
		request.setEndTime(LocalTime.of(12, 0));

		final String memberUsername = member.getUsername();
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			shiftService.createShift(group.getId(), memberUsername, request);
		});

		assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
		assertEquals("Chỉ Quản lý mới có quyền tạo ca làm việc", exception.getMessage());
	}
}
