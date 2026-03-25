package com.workshift.backend.me;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.me.dto.MyCalendarItemResponse;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

	private final MeService meService;

	public MeController(MeService meService) {
		this.meService = meService;
	}

	@GetMapping("/calendar")
	public ResponseEntity<ApiResponse<List<MyCalendarItemResponse>>> getMyCalendar(
			Authentication authentication,
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(value = "range", required = false) String range
	) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "Chưa xác thực");
		}

		LocalDate[] resolved = resolveRange(from, to, range);
		List<MyCalendarItemResponse> data = meService.getMyCalendar(authentication.getName(), resolved[0], resolved[1]);
		return ResponseEntity.ok(ApiResponse.ok("Lịch làm việc của tôi", data));
	}

	private LocalDate[] resolveRange(LocalDate from, LocalDate to, String range) {
		if (from != null || to != null) {
			if (from == null || to == null) {
				throw new BusinessException(HttpStatus.BAD_REQUEST, "Thiếu tham số from hoặc to");
			}
			if (from.isAfter(to)) {
				throw new BusinessException(HttpStatus.BAD_REQUEST, "from phải trước hoặc bằng to");
			}
			return new LocalDate[] {from, to};
		}

		if (range == null || range.isBlank()) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Thiếu tham số from/to hoặc range");
		}

		LocalDate today = LocalDate.now();
		String normalized = range.trim().toLowerCase();
		return switch (normalized) {
			case "week" -> {
				LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				LocalDate end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
				yield new LocalDate[] {start, end};
			}
			case "month" -> {
				LocalDate start = today.withDayOfMonth(1);
				LocalDate end = today.with(TemporalAdjusters.lastDayOfMonth());
				yield new LocalDate[] {start, end};
			}
			default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "range không hợp lệ (week/month)");
		};
	}
}

