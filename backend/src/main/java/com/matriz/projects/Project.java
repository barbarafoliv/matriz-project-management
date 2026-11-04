package com.matriz.projects;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Groups the team's tasks (FR-02).
 */
@Entity
@Table(name = "project")
class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 1000)
	private String description;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Project() {
	}

	Project(String name, String description, Instant now) {
		this.name = name;
		this.description = description;
		this.createdAt = now;
		this.updatedAt = now;
	}

	void change(String name, String description, Instant now) {
		this.name = name;
		this.description = description;
		this.updatedAt = now;
	}

	Long getId() {
		return this.id;
	}

	String getName() {
		return this.name;
	}

	String getDescription() {
		return this.description;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

	Instant getUpdatedAt() {
		return this.updatedAt;
	}

}
