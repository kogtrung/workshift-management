package com.workshift.backend.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
import com.workshift.backend.domain.Position;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.SalaryConfig;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.domain.User;
import com.workshift.backend.payroll.dto.PayrollResponse;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.PositionRepository;
import com.workshift.backend.repository.ShiftRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.salary.SalaryConfigRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PayrollServiceTest {

	@Autowired
	private PayrollService payrollService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private ShiftRepository shiftRepository;

	@Autowired
	private PositionRepository positionRepository;

	@Autowired
	private RegistrationRepository registrationRepository;

	@Autowired
	private SalaryConfigRepository salaryConfigRepository;

	private User manager;
	private User member;
	private Group group;
	private Position position;

	// Use a fixed month for deterministic tests
	private final int testMonth = 6;
	private final int testYear = 2026;

	@BeforeEach
	void setUp() {
		manager = new User();
		manager.setUsername("payroll_manager");
		manager.setEmail("payroll_manager@test.com");
		manager.setPassword("password");
		manager.setFullName("Payroll Manager");
		manager = userRepository.save(manager);

		member = new User();
		member.setUsername("payroll_member");
		member.setEmail("payroll_member@test.com");
		member.setPassword("password");
		member.setFullName("Payroll Member");
		member = userRepository.save(member);

		group = new Group();
		group.setName("Payroll Test Group");
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

		position = new Position();
		position.setGroup(group);
		position.setName("Pha chế");
		position = positionRepository.save(position);
	}

	// ===== Helpers =====

	private Shift createShift(LocalDate date, LocalTime start, LocalTime end) {
		Shift shift = new Shift();
		shift.setGroup(group);
		shift.setName("Ca test");
		shift.setDate(date);
		shift.setStartTime(start);
		shift.setEndTime(end);
		shift.setStatus(ShiftStatus.OPEN);
		return shiftRepository.save(shift);
	}

	private Registration createApprovedRegistration(Shift shift, User user) {
		Registration reg = new Registration();
		reg.setShift(shift);
		reg.setUser(user);
		reg.setPosition(position);
		reg.setStatus(RegistrationStatus.APPROVED);
		return registrationRepository.save(reg);
	}

	private SalaryConfig createPositionSalaryConfig(BigDecimal rate) {
		SalaryConfig config = new SalaryConfig();
		config.setGroup(group);
		config.setPosition(position);
		config.setHourlyRate(rate);
		config.setEffectiveDate(LocalDate.of(testYear, 1, 1));
		return salaryConfigRepository.save(config);
	}

	private SalaryConfig createUserSalaryConfig(User user, BigDecimal rate) {
		SalaryConfig config = new SalaryConfig();
		config.setGroup(group);
		config.setUser(user);
		config.setHourlyRate(rate);
		config.setEffectiveDate(LocalDate.of(testYear, 1, 1));
		return salaryConfigRepository.save(config);
	}

	// ===== Tests =====

	@Test
	void getPayroll_success() {
		// 2 ca: 8-12 (4h) và 13-17 (4h) → 8h tổng, rate 25000/h → pay 200000
		Shift shift1 = createShift(LocalDate.of(testYear, testMonth, 5), LocalTime.of(8, 0), LocalTime.of(12, 0));
		Shift shift2 = createShift(LocalDate.of(testYear, testMonth, 10), LocalTime.of(13, 0), LocalTime.of(17, 0));
		createApprovedRegistration(shift1, member);
		createApprovedRegistration(shift2, member);
		createPositionSalaryConfig(new BigDecimal("25000"));

		PayrollResponse response = payrollService.getPayroll(group.getId(), manager.getUsername(), testMonth, testYear);

		assertNotNull(response);
		assertEquals(testMonth, response.getMonth());
		assertEquals(testYear, response.getYear());
		assertEquals(1, response.getEntries().size());

		var entry = response.getEntries().get(0);
		assertEquals(member.getId(), entry.userId());
		assertEquals("Payroll Member", entry.fullName());
		assertEquals(2, entry.totalShifts());
		assertEquals(0, new BigDecimal("8.00").compareTo(entry.totalHours()));
		assertEquals(0, new BigDecimal("25000").compareTo(entry.hourlyRate()));
		assertEquals(0, new BigDecimal("200000.00").compareTo(entry.totalPay()));
	}

	@Test
	void getPayroll_userSpecificRateOverrides() {
		Shift shift = createShift(LocalDate.of(testYear, testMonth, 1), LocalTime.of(8, 0), LocalTime.of(12, 0));
		createApprovedRegistration(shift, member);

		// Position rate = 20000, User rate = 30000 → user rate ưu tiên
		createPositionSalaryConfig(new BigDecimal("20000"));
		createUserSalaryConfig(member, new BigDecimal("30000"));

		PayrollResponse response = payrollService.getPayroll(group.getId(), manager.getUsername(), testMonth, testYear);

		assertEquals(1, response.getEntries().size());
		var entry = response.getEntries().get(0);
		assertEquals(0, new BigDecimal("30000").compareTo(entry.hourlyRate()));
		assertEquals(0, new BigDecimal("120000.00").compareTo(entry.totalPay())); // 4h * 30000
	}

	@Test
	void getPayroll_noRegistrations_returnsEmptyEntries() {
		PayrollResponse response = payrollService.getPayroll(group.getId(), manager.getUsername(), testMonth, testYear);

		assertNotNull(response);
		assertTrue(response.getEntries().isEmpty());
	}

	@Test
	void getPayroll_noSalaryConfig_zeroRate() {
		Shift shift = createShift(LocalDate.of(testYear, testMonth, 15), LocalTime.of(9, 0), LocalTime.of(13, 0));
		createApprovedRegistration(shift, member);
		// Không tạo salary config → rate = 0

		PayrollResponse response = payrollService.getPayroll(group.getId(), manager.getUsername(), testMonth, testYear);

		assertEquals(1, response.getEntries().size());
		var entry = response.getEntries().get(0);
		assertEquals(0, BigDecimal.ZERO.compareTo(entry.hourlyRate()));
		assertEquals(0, BigDecimal.ZERO.compareTo(entry.totalPay()));
	}

	@Test
	void getPayroll_fail_notManager() {
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			payrollService.getPayroll(group.getId(), member.getUsername(), testMonth, testYear);
		});

		assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
		assertEquals("Chỉ Quản lý mới có quyền xem bảng lương", exception.getMessage());
	}
}
