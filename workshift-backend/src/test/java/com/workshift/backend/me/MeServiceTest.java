package com.workshift.backend.me;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.domain.Group;
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.me.dto.MyCalendarItemResponse;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MeServiceTest {

	@Autowired
	private MeService meService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private PositionRepository positionRepository;

	@Autowired
	private ShiftRepository shiftRepository;

	@Autowired
	private RegistrationRepository registrationRepository;

	private User member;
	private Group group;
	private Position position;

	@BeforeEach
	void setUp() {
		member = new User();
		member.setUsername("member_calendar");
		member.setEmail("member_calendar@test.com");
		member.setPassword("password");
		member.setFullName("Member Calendar");
		member = userRepository.save(member);

		User creator = new User();
		creator.setUsername("creator_calendar");
		creator.setEmail("creator_calendar@test.com");
		creator.setPassword("password");
		creator.setFullName("Creator Calendar");
		creator = userRepository.save(creator);

		group = new Group();
		group.setName("Calendar Group");
		group.setCreatedBy(creator);
		group = groupRepository.save(group);

		position = new Position();
		position.setGroup(group);
		position.setName("Pha chế");
		position.setColorCode("#FFFFFF");
		position = positionRepository.save(position);
	}

	@Test
	void getMyCalendar_returnsOnlyApprovedInRange() {
		LocalDate inRangeDate = LocalDate.of(2026, 3, 10);
		Shift shiftInRange = new Shift();
		shiftInRange.setGroup(group);
		shiftInRange.setName("Ca sáng");
		shiftInRange.setDate(inRangeDate);
		shiftInRange.setStartTime(LocalTime.of(8, 0));
		shiftInRange.setEndTime(LocalTime.of(12, 0));
		shiftInRange.setStatus(ShiftStatus.OPEN);
		shiftInRange = shiftRepository.save(shiftInRange);

		Registration approvedInRange = new Registration();
		approvedInRange.setShift(shiftInRange);
		approvedInRange.setUser(member);
		approvedInRange.setPosition(position);
		approvedInRange.setStatus(RegistrationStatus.APPROVED);
		registrationRepository.save(approvedInRange);

		Shift shiftOutOfRange = new Shift();
		shiftOutOfRange.setGroup(group);
		shiftOutOfRange.setName("Ca ngoài range");
		shiftOutOfRange.setDate(LocalDate.of(2026, 2, 10));
		shiftOutOfRange.setStartTime(LocalTime.of(8, 0));
		shiftOutOfRange.setEndTime(LocalTime.of(12, 0));
		shiftOutOfRange.setStatus(ShiftStatus.OPEN);
		shiftOutOfRange = shiftRepository.save(shiftOutOfRange);

		Registration approvedOutOfRange = new Registration();
		approvedOutOfRange.setShift(shiftOutOfRange);
		approvedOutOfRange.setUser(member);
		approvedOutOfRange.setPosition(position);
		approvedOutOfRange.setStatus(RegistrationStatus.APPROVED);
		registrationRepository.save(approvedOutOfRange);

		Registration pendingInRange = new Registration();
		pendingInRange.setShift(shiftInRange);
		pendingInRange.setUser(member);
		pendingInRange.setPosition(position);
		pendingInRange.setStatus(RegistrationStatus.PENDING);
		registrationRepository.save(pendingInRange);

		List<MyCalendarItemResponse> result = meService.getMyCalendar(
				member.getUsername(),
				LocalDate.of(2026, 3, 1),
				LocalDate.of(2026, 3, 31)
		);

		assertEquals(1, result.size());
		assertEquals("Ca sáng", result.get(0).shiftName());
	}
}
