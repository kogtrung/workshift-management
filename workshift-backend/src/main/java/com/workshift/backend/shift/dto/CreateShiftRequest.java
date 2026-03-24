package com.workshift.backend.shift.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateShiftRequest {

	@NotBlank(message = "Tên ca làm việc không được để trống")
	private String name;

	@NotNull(message = "Ngày làm việc không được để trống")
	private LocalDate date;

	@NotNull(message = "Giờ bắt đầu không được để trống")
	private LocalTime startTime;

	@NotNull(message = "Giờ kết thúc không được để trống")
	private LocalTime endTime;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}
}
