package com.workshift.backend.availability;

import java.time.DayOfWeek;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.availability.dto.AvailabilityItemRequest;
import com.workshift.backend.availability.dto.AvailabilityResponse;
import com.workshift.backend.availability.dto.UpdateAvailabilityRequest;

@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

	@Mock
	private AvailabilityService availabilityService;

	@InjectMocks
	private AvailabilityController availabilityController;

	@Test
	void getAvailability_ReturnsOk() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("member", "N/A", java.util.List.of(() -> "ROLE_USER"));
		AvailabilityResponse res = new AvailabilityResponse(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
		when(availabilityService.getMyAvailability("member")).thenReturn(List.of(res));

		var response = availabilityController.getAvailability(authentication);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		ApiResponse<List<AvailabilityResponse>> body = response.getBody();
		assertEquals(1L, body.data().get(0).id());
	}

	@Test
	void updateAvailability_ReturnsOk() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("member", "N/A", java.util.List.of(() -> "ROLE_USER"));
		AvailabilityItemRequest reqItem = new AvailabilityItemRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
		UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(List.of(reqItem));
		AvailabilityResponse res = new AvailabilityResponse(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

		when(availabilityService.updateMyAvailability(eq("member"), any(UpdateAvailabilityRequest.class))).thenReturn(List.of(res));

		var response = availabilityController.updateAvailability(authentication, req);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		ApiResponse<List<AvailabilityResponse>> body = response.getBody();
		assertEquals(1L, body.data().get(0).id());
	}
}
