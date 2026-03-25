package com.workshift.backend.me;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.RegistrationStatus;
import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.me.dto.MyCalendarItemResponse;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

	@Mock
	private MeService meService;

	@InjectMocks
	private MeController meController;

	@Test
	void getMyCalendar_returnsOk() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("member", null, List.of());
		LocalDate from = LocalDate.of(2026, 3, 1);
		LocalDate to = LocalDate.of(2026, 3, 31);

		MyCalendarItemResponse item = new MyCalendarItemResponse(
				100L,
				10L,
				"Test Group",
				200L,
				"Ca sáng",
				LocalDate.of(2026, 3, 10),
				LocalTime.of(8, 0),
				LocalTime.of(12, 0),
				ShiftStatus.OPEN,
				300L,
				"Pha chế",
				"#FFFFFF",
				RegistrationStatus.APPROVED
		);

		when(meService.getMyCalendar("member", from, to)).thenReturn(List.of(item));

		var response = meController.getMyCalendar(authentication, from, to, null);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(200, response.getBody().status());
		assertEquals(1, response.getBody().data().size());
		assertEquals(200L, response.getBody().data().get(0).shiftId());
	}

	@Test
	void getMyCalendar_unauthorized_throwsBusinessException() {
		BusinessException ex = assertThrows(BusinessException.class, () -> meController.getMyCalendar(null, null, null, "week"));
		assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
	}
}

