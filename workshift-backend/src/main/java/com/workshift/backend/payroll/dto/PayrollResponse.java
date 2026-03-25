package com.workshift.backend.payroll.dto;

import java.math.BigDecimal;
import java.util.List;

public class PayrollResponse {

	private int month;
	private int year;
	private Long groupId;
	private List<PayrollEntry> entries;

	public record PayrollEntry(
			Long userId,
			String fullName,
			int totalShifts,
			BigDecimal totalHours,
			BigDecimal hourlyRate,
			BigDecimal totalPay
	) {
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public List<PayrollEntry> getEntries() {
		return entries;
	}

	public void setEntries(List<PayrollEntry> entries) {
		this.entries = entries;
	}
}
