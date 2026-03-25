package com.workshift.backend.domain;

import com.workshift.backend.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "template_requirements", uniqueConstraints = {
		@UniqueConstraint(name = "uk_template_requirement", columnNames = { "template_id", "position_id" })
})
public class TemplateRequirement extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "template_id", nullable = false)
	private ShiftTemplate template;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "position_id", nullable = false)
	private Position position;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	public ShiftTemplate getTemplate() {
		return template;
	}

	public void setTemplate(ShiftTemplate template) {
		this.template = template;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
