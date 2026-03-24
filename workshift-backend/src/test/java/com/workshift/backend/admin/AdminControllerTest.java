package com.workshift.backend.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.workshift.backend.common.api.ApiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

	@InjectMocks
	private AdminController adminController;

	@Test
	void pingAdmin_ReturnsOk() {
		var response = adminController.pingAdmin();
		assertEquals(HttpStatus.OK, response.getStatusCode());
		
		ApiResponse<String> body = response.getBody();
		assertEquals(200, body.status());
		assertEquals("ADMIN connection successful", body.data());
	}
}
