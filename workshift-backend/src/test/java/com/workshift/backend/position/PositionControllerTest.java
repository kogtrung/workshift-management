package com.workshift.backend.position;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshift.backend.position.dto.CreatePositionRequest;
import com.workshift.backend.position.dto.PositionResponse;

@WebMvcTest(PositionController.class)
class PositionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PositionService positionService;

	@Test
	@WithMockUser(username = "manager")
	void createPosition_ReturnsCreated() throws Exception {
		CreatePositionRequest req = new CreatePositionRequest("Pha chế", "#FFFFFF");
		PositionResponse res = new PositionResponse(1L, 10L, "Pha chế", "#FFFFFF");

		when(positionService.createPosition(eq("manager"), eq(10L), any(CreatePositionRequest.class))).thenReturn(res);

		mockMvc.perform(post("/api/v1/groups/10/positions")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").value(1L))
				.andExpect(jsonPath("$.data.name").value("Pha chế"));
	}

	@Test
	@WithMockUser(username = "member")
	void getPositions_ReturnsOk() throws Exception {
		PositionResponse res = new PositionResponse(1L, 10L, "Pha chế", "#FFFFFF");
		when(positionService.getPositions("member", 10L)).thenReturn(List.of(res));

		mockMvc.perform(get("/api/v1/groups/10/positions")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(1L));
	}
}
