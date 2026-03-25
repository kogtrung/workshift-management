package com.workshift.backend.alert.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class UnderstaffedAlertResponse {

	private Long shiftId;
	private String shiftName;
	private LocalDate date;
	private LocalTime startTime;
	private LocalTime endTime;
	private List<PositionShortage> shortages;
	private int totalRequired;
	private int totalApproved;

	public record PositionShortage(
			Long positionId,
			String positionName,
			int required,
			int approved,
			int shortage
	) {
	}

	public Long getShiftId() {
		return shiftId;
	}

	public void setShiftId(Long shiftId) {
		this.shiftId = shiftId;
	}

	public String getShiftName() {
		return shiftName;
	}

	public void setShiftName(String shiftName) {
		this.shiftName = shiftName;
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

	public List<PositionShortage> getShortages() {
		return shortages;
	}

	public void setShortages(List<PositionShortage> shortages) {
		this.shortages = shortages;
	}

	public int getTotalRequired() {
		return totalRequired;
	}

	public void setTotalRequired(int totalRequired) {
		this.totalRequired = totalRequired;
	}

	public int getTotalApproved() {
		return totalApproved;
	}

	public void setTotalApproved(int totalApproved) {
		this.totalApproved = totalApproved;
	}
}
