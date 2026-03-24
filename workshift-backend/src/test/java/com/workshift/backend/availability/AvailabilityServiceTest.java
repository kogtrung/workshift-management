package com.workshift.backend.availability;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.workshift.backend.common.exception.BusinessException;
import com.workshift.backend.domain.Availability;
import com.workshift.backend.domain.User;
import com.workshift.backend.repository.AvailabilityRepository;
import com.workshift.backend.repository.UserRepository;
import com.workshift.backend.availability.dto.AvailabilityItemRequest;
import com.workshift.backend.availability.dto.AvailabilityResponse;
import com.workshift.backend.availability.dto.UpdateAvailabilityRequest;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

	@Mock
	private AvailabilityRepository availabilityRepository;
	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AvailabilityService availabilityService;

	private User mockUser;

	@BeforeEach
	void setUp() {
		mockUser = new User();
		mockUser.setId(1L);
		mockUser.setUsername("memberuser");
	}

	@Test
	void getMyAvailability_Success() {
		when(userRepository.findByUsername("memberuser")).thenReturn(Optional.of(mockUser));

		Availability a1 = new Availability();
		a1.setId(10L);
		a1.setDayOfWeek(DayOfWeek.MONDAY);
		a1.setStartTime(LocalTime.of(8, 0));
		a1.setEndTime(LocalTime.of(12, 0));

		when(availabilityRepository.findByUserId(1L)).thenReturn(List.of(a1));

		List<AvailabilityResponse> res = availabilityService.getMyAvailability("memberuser");
		assertThat(res).hasSize(1);
		assertThat(res.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
	}

	@Test
	void updateMyAvailability_Success() {
		when(userRepository.findByUsername("memberuser")).thenReturn(Optional.of(mockUser));

		AvailabilityItemRequest reqItem = new AvailabilityItemRequest(DayOfWeek.TUESDAY, LocalTime.of(13, 0), LocalTime.of(17, 0));
		UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(List.of(reqItem));

		Availability savedAvailability = new Availability();
		savedAvailability.setId(100L);
		savedAvailability.setDayOfWeek(DayOfWeek.TUESDAY);
		savedAvailability.setStartTime(LocalTime.of(13, 0));
		savedAvailability.setEndTime(LocalTime.of(17, 0));

		when(availabilityRepository.saveAll(any())).thenReturn(List.of(savedAvailability));

		List<AvailabilityResponse> res = availabilityService.updateMyAvailability("memberuser", req);

		verify(availabilityRepository, times(1)).deleteByUserId(1L);
		verify(availabilityRepository, times(1)).saveAll(any());
		
		assertThat(res).hasSize(1);
		assertThat(res.get(0).dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
	}

	@Test
	void updateMyAvailability_FailsIfEndTimeBeforeStartTime() {
		when(userRepository.findByUsername("memberuser")).thenReturn(Optional.of(mockUser));

		AvailabilityItemRequest reqItem = new AvailabilityItemRequest(DayOfWeek.TUESDAY, LocalTime.of(13, 0), LocalTime.of(10, 0));
		UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(List.of(reqItem));

		assertThatThrownBy(() -> availabilityService.updateMyAvailability("memberuser", req))
				.isInstanceOf(BusinessException.class)
				.extracting("status")
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}
}
