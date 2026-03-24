package com.workshift.backend.shift.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.workshift.backend.domain.ShiftStatus;

public class CreateShiftResponse {

	private Long id;
	private Long groupId;
	private Long templateId;
	private String name;
	private LocalDate date;
	private LocalTime startTime;
	private LocalTime endTime;
	private String note;
	private ShiftStatus status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public Long getTemplateId() {
		return templateId;
	}

	public void setTemplateId(Long templateId) {
		this.templateId = templateId;
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

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public ShiftStatus getStatus() {
		return status;
	}

	public void setStatus(ShiftStatus status) {
		this.status = status;
	}
}
