package com.workshift.backend.shift.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.workshift.backend.domain.ShiftStatus;
import com.workshift.backend.shiftrequirement.dto.ShiftRequirementResponse;

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
	private List<ShiftRequirementResponse> requirements;
	private int totalRequired;
	private List<AssignedMemberResponse> assignedMembers;

	public CreateShiftResponse() {
		this.requirements = new java.util.ArrayList<>();
		this.assignedMembers = new java.util.ArrayList<>();
	}

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

	public List<ShiftRequirementResponse> getRequirements() {
		return requirements;
	}

	public void setRequirements(List<ShiftRequirementResponse> requirements) {
		this.requirements = requirements;
	}

	public int getTotalRequired() {
		return totalRequired;
	}

	public void setTotalRequired(int totalRequired) {
		this.totalRequired = totalRequired;
	}

	public List<AssignedMemberResponse> getAssignedMembers() {
		return assignedMembers;
	}

	public void setAssignedMembers(List<AssignedMemberResponse> assignedMembers) {
		this.assignedMembers = assignedMembers;
	}
}
