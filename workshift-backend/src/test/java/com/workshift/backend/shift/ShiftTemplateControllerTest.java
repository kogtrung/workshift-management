package com.workshift.backend.shift;

import java.time.LocalTime;
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
import com.workshift.backend.shift.dto.template.CreateShiftTemplateRequest;
import com.workshift.backend.shift.dto.template.ShiftTemplateResponse;

@WebMvcTest(ShiftTemplateController.class)
class ShiftTemplateControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ShiftTemplateService shiftTemplateService;

	@MockBean
	private com.workshift.backend.auth.jwt.JwtService jwtService;

	@MockBean
	private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

	@MockBean
	private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

	@Test
	@WithMockUser(username = "manager")
	void createTemplate_ReturnsCreated() throws Exception {
		CreateShiftTemplateRequest req = new CreateShiftTemplateRequest("Ca Sáng", LocalTime.of(8, 0), LocalTime.of(12, 0), "Mô tả");
		ShiftTemplateResponse res = new ShiftTemplateResponse(1L, 10L, "Ca Sáng", LocalTime.of(8, 0), LocalTime.of(12, 0), "Mô tả");

		when(shiftTemplateService.createTemplate(eq("manager"), eq(10L), any(CreateShiftTemplateRequest.class))).thenReturn(res);
		
		mockMvc.perform(post("/api/v1/groups/10/shift-templates")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").value(1L))
				.andExpect(jsonPath("$.data.name").value("Ca Sáng"));
	}

	@Test
	@WithMockUser(username = "member")
	void getTemplates_ReturnsOk() throws Exception {
		ShiftTemplateResponse res = new ShiftTemplateResponse(1L, 10L, "Ca Sáng", LocalTime.of(8, 0), LocalTime.of(12, 0), "Mô tả");
		when(shiftTemplateService.getTemplates("member", 10L)).thenReturn(List.of(res));

		mockMvc.perform(get("/api/v1/groups/10/shift-templates")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(1L));
	}
}
