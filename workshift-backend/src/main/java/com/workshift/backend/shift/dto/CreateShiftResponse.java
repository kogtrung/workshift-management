package com.workshift.backend.shift.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.workshift.backend.domain.ShiftStatus;

public class CreateShiftResponse {

	private Long id;
	private String name;
	private LocalDate date;
	private LocalTime startTime;
	private LocalTime endTime;
	private ShiftStatus status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public ShiftStatus getStatus() {
		return status;
	}

	public void setStatus(ShiftStatus status) {
		this.status = status;
	}
}
