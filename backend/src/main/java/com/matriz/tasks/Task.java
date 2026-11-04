package com.matriz.tasks;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A unit of work in a project (FR-04, FR-05).
 */
@Entity
@Table(name = "task")
class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false, updatable = false)
	private Long projectId;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(length = 2000)
	private String description;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Priority priority;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Task() {
	}

	Task(long projectId, String title, String description, LocalDate dueDate, Priority priority, Instant now) {
		this.projectId = projectId;
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
		this.priority = priority;
		this.status = TaskStatus.TODO;
		this.createdAt = now;
		this.updatedAt = now;
	}

	void change(String title, String description, LocalDate dueDate, Priority priority, Instant now) {
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
		this.priority = priority;
		this.updatedAt = now;
	}

	void changeStatus(TaskStatus status, Instant now) {
		this.status = status;
		this.updatedAt = now;
	}

	/**
	 * Late: due before today and not done (FR-08). Not late when due today or without a
	 * due date (EC-05).
	 */
	boolean isLateOn(LocalDate today) {
		return this.dueDate != null && this.dueDate.isBefore(today) && this.status != TaskStatus.DONE;
	}

	Long getId() {
		return this.id;
	}

	Long getProjectId() {
		return this.projectId;
	}

	String getTitle() {
		return this.title;
	}

	String getDescription() {
		return this.description;
	}

	LocalDate getDueDate() {
		return this.dueDate;
	}

	Priority getPriority() {
		return this.priority;
	}

	TaskStatus getStatus() {
		return this.status;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

	Instant getUpdatedAt() {
		return this.updatedAt;
	}

}
