package com.workshift.backend.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.GroupMember;
import com.workshift.backend.domain.GroupMemberStatus;
import com.workshift.backend.domain.GroupRole;
import com.workshift.backend.domain.Registration;
import com.workshift.backend.domain.SalaryConfig;
import com.workshift.backend.domain.Shift;
import com.workshift.backend.domain.User;
import com.workshift.backend.payroll.dto.PayrollResponse;
import com.workshift.backend.payroll.dto.PayrollResponse.PayrollEntry;
import com.workshift.backend.registration.RegistrationRepository;
import com.workshift.backend.repository.GroupMemberRepository;
import com.workshift.backend.repository.GroupRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.salary.SalaryConfigRepository;

@Service
public class PayrollService {

	private final RegistrationRepository registrationRepository;
	private final SalaryConfigRepository salaryConfigRepository;
	private final UserRepository userRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;

	public PayrollService(RegistrationRepository registrationRepository,
						  SalaryConfigRepository salaryConfigRepository,
						  UserRepository userRepository,
						  GroupRepository groupRepository,
						  GroupMemberRepository groupMemberRepository) {
		this.registrationRepository = registrationRepository;
		this.salaryConfigRepository = salaryConfigRepository;
		this.userRepository = userRepository;
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
	}

	@Transactional(readOnly = true)
	public PayrollResponse getPayroll(Long groupId, String username, int month, int year) {
		// 1. Validate
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));

		groupRepository.findById(groupId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm"));

		GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Bạn không phải là thành viên của nhóm này"));

		if (member.getRole() != GroupRole.MANAGER || member.getStatus() != GroupMemberStatus.APPROVED) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Chỉ Quản lý mới có quyền xem bảng lương");
		}

		if (month < 1 || month > 12) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Tháng không hợp lệ (1-12)");
		}

		// 2. Tính khoảng ngày của tháng
		YearMonth ym = YearMonth.of(year, month);
		LocalDate from = ym.atDay(1);
		LocalDate to = ym.atEndOfMonth();

		// 3. Lấy APPROVED registrations trong tháng
		List<Registration> registrations = registrationRepository
				.findApprovedByGroupAndDateRange(groupId, from, to);

		// 4. Load salary configs có hiệu lực
		List<SalaryConfig> salaryConfigs = salaryConfigRepository
				.findByGroupIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(groupId, to);

		// 5. Nhóm registrations theo userId
		Map<Long, UserPayrollData> userDataMap = new LinkedHashMap<>();

		for (Registration reg : registrations) {
			User regUser = reg.getUser();
			Shift shift = reg.getShift();

			long shiftMinutes = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
			BigDecimal hours = BigDecimal.valueOf(shiftMinutes)
					.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

			UserPayrollData data = userDataMap.computeIfAbsent(regUser.getId(),
					id -> new UserPayrollData(regUser.getId(), regUser.getFullName()));
			data.totalShifts++;
			data.totalHours = data.totalHours.add(hours);

			// Lấy hourly rate cho registration này (position-specific)
			// Sẽ override bằng user-specific sau
			if (data.positionId == null) {
				data.positionId = reg.getPosition().getId();
			}
		}

		// 6. Tính hourly rate và totalPay cho mỗi user
		List<PayrollEntry> entries = new ArrayList<>();

		for (UserPayrollData data : userDataMap.values()) {
			BigDecimal hourlyRate = resolveHourlyRate(data.userId, data.positionId, salaryConfigs);
			BigDecimal totalPay = data.totalHours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);

			entries.add(new PayrollEntry(
					data.userId,
					data.fullName,
					data.totalShifts,
					data.totalHours,
					hourlyRate,
					totalPay
			));
		}

		// 7. Build response
		PayrollResponse response = new PayrollResponse();
		response.setMonth(month);
		response.setYear(year);
		response.setGroupId(groupId);
		response.setEntries(entries);

		return response;
	}

	/**
	 * Tìm hourly rate cho nhân viên.
	 * Ưu tiên: user-specific > position-specific > 0 (mặc định).
	 * Configs đã sort theo effectiveDate DESC → lấy bản ghi đầu tiên khớp.
	 */
	private BigDecimal resolveHourlyRate(Long userId, Long positionId, List<SalaryConfig> configs) {
		BigDecimal userRate = null;
		BigDecimal positionRate = null;

		for (SalaryConfig config : configs) {
			// user-specific
			if (config.getUser() != null && config.getUser().getId().equals(userId) && userRate == null) {
				userRate = config.getHourlyRate();
			}
			// position-specific
			if (config.getPosition() != null && config.getPosition().getId().equals(positionId) && positionRate == null) {
				positionRate = config.getHourlyRate();
			}
			if (userRate != null && positionRate != null) {
				break;
			}
		}

		if (userRate != null) {
			return userRate;
		}
		if (positionRate != null) {
			return positionRate;
		}
		return BigDecimal.ZERO;
	}

	private static class UserPayrollData {
		Long userId;
		String fullName;
		int totalShifts = 0;
		BigDecimal totalHours = BigDecimal.ZERO;
		Long positionId;

		UserPayrollData(Long userId, String fullName) {
			this.userId = userId;
			this.fullName = fullName;
		}
	}
}
