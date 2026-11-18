package com.matriz.tasks;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

/**
 * A user assigned to a task (FR-15).
 */
@Entity
@Table(name = "task_assignee")
@IdClass(TaskAssignee.Key.class)
class TaskAssignee implements Persistable<TaskAssignee.Key> {

	@Id
	@Column(name = "task_id")
	private Long taskId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/**
	 * The key is set before saving, so Spring Data would otherwise merge a new instance: an
	 * existing row with the same key would be updated instead of rejected by the primary key.
	 */
	@Transient
	private boolean isNew = true;

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

	@Override
	public Key getId() {
		return new Key(this.taskId, this.userId);
	}

	@Override
	public boolean isNew() {
		return this.isNew;
	}

	@PostLoad
	@PostPersist
	void markSaved() {
		this.isNew = false;
	}

	record Key(Long taskId, Long userId) implements Serializable {

		Key() {
			this(null, null);
		}

	}

}
