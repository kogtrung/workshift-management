package com.workshift.backend.shift;

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
import com.workshift.backend.shift.dto.template.CreateShiftTemplateRequest;
import com.workshift.backend.shift.dto.template.ShiftTemplateResponse;

@ExtendWith(MockitoExtension.class)
class ShiftTemplateControllerTest {

	@Mock
	private ShiftTemplateService shiftTemplateService;

	@InjectMocks
	private ShiftTemplateController shiftTemplateController;

	@Test
	void createTemplate_ReturnsCreated() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("manager", "N/A", java.util.List.of(() -> "ROLE_USER"));
		CreateShiftTemplateRequest req = new CreateShiftTemplateRequest("Ca Sáng", LocalTime.of(8, 0), LocalTime.of(12, 0), "Mô tả");
		ShiftTemplateResponse res = new ShiftTemplateResponse(1L, 10L, "Ca Sáng", LocalTime.of(8, 0), LocalTime.of(12, 0), "Mô tả");

		when(shiftTemplateService.createTemplate(eq("manager"), eq(10L), any(CreateShiftTemplateRequest.class))).thenReturn(res);

		var response = shiftTemplateController.createTemplate(authentication, 10L, req);
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		ApiResponse<ShiftTemplateResponse> body = response.getBody();
		assertEquals(201, body.status());
		assertEquals(1L, body.data().id());
		assertEquals("Ca Sáng", body.data().name());
	}

	@Test
	void getTemplates_ReturnsOk() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("member", "N/A", java.util.List.of(() -> "ROLE_USER"));
		ShiftTemplateResponse res = new ShiftTemplateResponse(1L, 10L, "Ca Sáng", LocalTime.of(8, 0), LocalTime.of(12, 0), "Mô tả");
		when(shiftTemplateService.getTemplates("member", 10L)).thenReturn(List.of(res));

		var response = shiftTemplateController.getTemplates(authentication, 10L);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		ApiResponse<List<ShiftTemplateResponse>> body = response.getBody();
		assertEquals(200, body.status());
		assertEquals(1L, body.data().get(0).id());
	}
}
