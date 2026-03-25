package com.workshift.backend.shift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Availability;
import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftRequirement;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.ShiftTemplate;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.AvailabilityRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.ShiftRequirementRepository;
import com.workshift.backend.repository.ShiftTemplateRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.shift.dto.AvailableShiftResponse;
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

	@Autowired
	private ShiftTemplateRepository shiftTemplateRepository;

	@Autowired
	private AvailabilityRepository availabilityRepository;

	@Autowired
	private ShiftRequirementRepository shiftRequirementRepository;

	@Autowired
	private PositionRepository positionRepository;

	private User manager;
	private User member;
	private Group group;

	@BeforeEach
	void setUp() {
		manager = new User();
		manager.setUsername("manager_test");
		manager.setEmail("manager@test.com");
		manager.setPassword("password");
		manager.setFullName("Manager Test");
		manager = userRepository.save(manager);

		member = new User();
		member.setUsername("member_test");
		member.setEmail("member@test.com");
		member.setPassword("password");
		member.setFullName("Member Test");
		member = userRepository.save(member);

		group = new Group();
		group.setName("Test Group");
		group.setCreatedBy(manager);
		group = groupRepository.save(group);

		GroupMember managerMember = new GroupMember();
		managerMember.setGroup(group);
		managerMember.setUser(manager);
		managerMember.setRole(GroupRole.MANAGER);
		managerMember.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(managerMember);

		GroupMember groupMember = new GroupMember();
		groupMember.setGroup(group);
		groupMember.setUser(member);
		groupMember.setRole(GroupRole.MEMBER);
		groupMember.setStatus(GroupMemberStatus.APPROVED);
		groupMemberRepository.save(groupMember);
	}

	// ===== Helper methods =====

	private Shift createShiftOnDay(DayOfWeek day, LocalTime start, LocalTime end, ShiftStatus status) {
		LocalDate date = LocalDate.now().with(TemporalAdjusters.nextOrSame(day));
		Shift shift = new Shift();
		shift.setGroup(group);
		shift.setName("Test Shift");
		shift.setDate(date);
		shift.setStartTime(start);
		shift.setEndTime(end);
		shift.setStatus(status);
		return shiftRepository.save(shift);
	}

	private void addAvailability(User user, DayOfWeek day, LocalTime start, LocalTime end) {
		Availability avail = new Availability();
		avail.setUser(user);
		avail.setDayOfWeek(day);
		avail.setStartTime(start);
		avail.setEndTime(end);
		availabilityRepository.save(avail);
	}

	private void addShiftRequirement(Shift shift, int quantity) {
		Position position = new Position();
		position.setGroup(group);
		position.setName("Staff-" + System.nanoTime());
		position = positionRepository.save(position);

		ShiftRequirement req = new ShiftRequirement();
		req.setShift(shift);
		req.setPosition(position);
		req.setQuantity(quantity);
		shiftRequirementRepository.save(req);
	}

	// ===== Existing tests =====

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
		CreateShiftRequest request1 = new CreateShiftRequest();
		request1.setName("Ca 1");
		request1.setDate(LocalDate.now().plusDays(1));
		request1.setStartTime(LocalTime.of(8, 0));
		request1.setEndTime(LocalTime.of(12, 0));
		shiftService.createShift(group.getId(), manager.getUsername(), request1);

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

	@Test
	void createShift_withTemplate_shouldUseTemplateTimesAndDefaultName() {
		ShiftTemplate template = new ShiftTemplate();
		template.setGroup(group);
		template.setName("Ca Template");
		template.setStartTime(LocalTime.of(7, 0));
		template.setEndTime(LocalTime.of(11, 0));
		template = shiftTemplateRepository.save(template);

		CreateShiftRequest request = new CreateShiftRequest();
		request.setDate(LocalDate.now().plusDays(2));
		request.setTemplateId(template.getId());

		CreateShiftResponse response = shiftService.createShift(group.getId(), manager.getUsername(), request);

		assertNotNull(response.getId());
		assertEquals("Ca Template", response.getName());
		assertEquals(LocalTime.of(7, 0), response.getStartTime());
		assertEquals(LocalTime.of(11, 0), response.getEndTime());
		assertEquals(template.getId(), response.getTemplateId());
	}

	@Test
	void createShift_withTemplate_shouldRejectManualTimes() {
		ShiftTemplate template = new ShiftTemplate();
		template.setGroup(group);
		template.setName("Ca Template 2");
		template.setStartTime(LocalTime.of(7, 0));
		template.setEndTime(LocalTime.of(11, 0));
		template = shiftTemplateRepository.save(template);

		CreateShiftRequest request = new CreateShiftRequest();
		request.setDate(LocalDate.now().plusDays(2));
		request.setTemplateId(template.getId());
		request.setStartTime(LocalTime.of(8, 0));
		request.setEndTime(LocalTime.of(12, 0));

		BusinessException exception = assertThrows(BusinessException.class, () -> {
			shiftService.createShift(group.getId(), manager.getUsername(), request);
		});

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("Đã chọn ca mẫu thì không được nhập giờ thủ công", exception.getMessage());
	}

	@Test
	void createShiftsBulk_shouldCreateMultipleShifts() {
		CreateShiftRequest request1 = new CreateShiftRequest();
		request1.setName("Ca 1");
		request1.setDate(LocalDate.now().plusDays(3));
		request1.setStartTime(LocalTime.of(8, 0));
		request1.setEndTime(LocalTime.of(12, 0));

		CreateShiftRequest request2 = new CreateShiftRequest();
		request2.setName("Ca 2");
		request2.setDate(LocalDate.now().plusDays(4));
		request2.setStartTime(LocalTime.of(13, 0));
		request2.setEndTime(LocalTime.of(17, 0));

		var responses = shiftService.createShiftsBulk(group.getId(), manager.getUsername(), List.of(request1, request2));

		assertEquals(2, responses.size());
		assertNotNull(responses.get(0).getId());
		assertNotNull(responses.get(1).getId());
	}

	// ===== B11: getAvailableShifts tests =====

	@Test
	void getAvailableShifts_returnsShiftMatchingAvailability() {
		DayOfWeek day = DayOfWeek.MONDAY;
		Shift shift = createShiftOnDay(day, LocalTime.of(8, 0), LocalTime.of(12, 0), ShiftStatus.OPEN);
		addShiftRequirement(shift, 3);
		addAvailability(member, day, LocalTime.of(7, 0), LocalTime.of(13, 0));

		List<AvailableShiftResponse> result = shiftService.getAvailableShifts(group.getId(), member.getUsername());

		assertFalse(result.isEmpty(), "Phải có ít nhất 1 ca phù hợp");
		assertTrue(result.stream().anyMatch(r -> r.getId().equals(shift.getId())));
		assertEquals(3, result.stream().filter(r -> r.getId().equals(shift.getId()))
				.findFirst().get().getTotalSlots());
	}

	@Test
	void getAvailableShifts_excludesNonOpenShift() {
		DayOfWeek day = DayOfWeek.TUESDAY;
		Shift lockedShift = createShiftOnDay(day, LocalTime.of(8, 0), LocalTime.of(12, 0), ShiftStatus.LOCKED);
		addShiftRequirement(lockedShift, 2);
		addAvailability(member, day, LocalTime.of(7, 0), LocalTime.of(13, 0));

		List<AvailableShiftResponse> result = shiftService.getAvailableShifts(group.getId(), member.getUsername());

		assertTrue(result.stream().noneMatch(r -> r.getId().equals(lockedShift.getId())),
				"Ca LOCKED không được hiển thị");
	}

	@Test
	void getAvailableShifts_excludesShiftNotMatchingAvailability() {
		// Ca vào thứ 4, nhưng availability chỉ có thứ 3
		Shift shift = createShiftOnDay(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), ShiftStatus.OPEN);
		addShiftRequirement(shift, 2);
		addAvailability(member, DayOfWeek.TUESDAY, LocalTime.of(7, 0), LocalTime.of(13, 0));

		List<AvailableShiftResponse> result = shiftService.getAvailableShifts(group.getId(), member.getUsername());

		assertTrue(result.stream().noneMatch(r -> r.getId().equals(shift.getId())),
				"Ca không khớp ngày trong tuần không được hiển thị");
	}

	@Test
	void getAvailableShifts_excludesShiftWithNoRequirements() {
		DayOfWeek day = DayOfWeek.THURSDAY;
		// Ca OPEN, availability khớp nhưng KHÔNG có ShiftRequirement
		Shift shift = createShiftOnDay(day, LocalTime.of(8, 0), LocalTime.of(12, 0), ShiftStatus.OPEN);
		addAvailability(member, day, LocalTime.of(7, 0), LocalTime.of(13, 0));

		List<AvailableShiftResponse> result = shiftService.getAvailableShifts(group.getId(), member.getUsername());

		assertTrue(result.stream().noneMatch(r -> r.getId().equals(shift.getId())),
				"Ca không có ShiftRequirement không được hiển thị");
	}
}
