package com.matriz.tasks;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * A user assigned to a task (FR-15).
 */
@Entity
@Table(name = "task_assignee")
@IdClass(TaskAssignee.Key.class)
class TaskAssignee {

	@Id
	@Column(name = "task_id")
	private Long taskId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected TaskAssignee() {
	}

	TaskAssignee(long taskId, long userId, Instant now) {
		this.taskId = taskId;
		this.userId = userId;
		this.createdAt = now;
	}

	Long getTaskId() {
		return this.taskId;
	}

	Long getUserId() {
		return this.userId;
	}

	record Key(Long taskId, Long userId) implements Serializable {

		Key() {
			this(null, null);
		}

	}

}
