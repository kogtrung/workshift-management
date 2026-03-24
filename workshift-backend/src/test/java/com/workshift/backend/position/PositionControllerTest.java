package com.workshift.backend.position;

import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.http.HttpStatus;

import com.workshift.backend.common.api.ApiResponse;
import com.workshift.backend.position.dto.CreatePositionRequest;
import com.workshift.backend.position.dto.PositionResponse;

@ExtendWith(MockitoExtension.class)
class PositionControllerTest {

	@Mock
	private PositionService positionService;

	@InjectMocks
	private PositionController positionController;
 

	@Test
	void createPosition_ReturnsCreated() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("manager", null, List.of());
		CreatePositionRequest req = new CreatePositionRequest("Pha chế", "#FFFFFF");
		PositionResponse res = new PositionResponse(1L, 10L, "Pha chế", "#FFFFFF");

		when(positionService.createPosition("manager", 10L, req)).thenReturn(res);

		var response = positionController.createPosition(authentication, 10L, req);
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		ApiResponse<PositionResponse> body = response.getBody();
		assertEquals(201, body.status());
		assertEquals(1L, body.data().id());
		assertEquals("Pha chế", body.data().name());
	}

	@Test
	void getPositions_ReturnsOk() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("member", null, List.of());
		PositionResponse res = new PositionResponse(1L, 10L, "Pha chế", "#FFFFFF");
		when(positionService.getPositions("member", 10L)).thenReturn(List.of(res));

		var response = positionController.getPositions(authentication, 10L);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		ApiResponse<List<PositionResponse>> body = response.getBody();
		assertEquals(200, body.status());
		assertEquals(1L, body.data().get(0).id());
	}
}
