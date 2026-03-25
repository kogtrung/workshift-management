package com.workshift.backend.shift.dto;

public class AssignedMemberResponse {
	private Long userId;
	private String fullName;
	private String username;
	private Long positionId;
	private String positionName;
	private String colorCode;

	public AssignedMemberResponse() {
	}

	public AssignedMemberResponse(Long userId, String fullName, String username, Long positionId, String positionName, String colorCode) {
		this.userId = userId;
		this.fullName = fullName;
		this.username = username;
		this.positionId = positionId;
		this.positionName = positionName;
		this.colorCode = colorCode;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Long getPositionId() {
		return positionId;
	}

	public void setPositionId(Long positionId) {
		this.positionId = positionId;
	}

	public String getPositionName() {
		return positionName;
	}

	public void setPositionName(String positionName) {
		this.positionName = positionName;
	}

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
	}
}
