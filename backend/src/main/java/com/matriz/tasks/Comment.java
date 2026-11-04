package com.matriz.tasks;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A message about a task (FR-09). The author is null after their account is deleted (US-09).
 */
@Entity
@Table(name = "comment")
class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "task_id", nullable = false, updatable = false)
	private Long taskId;

	@Column(name = "author_id", updatable = false)
	private Long authorId;

	@Column(nullable = false, length = 2000, updatable = false)
	private String content;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Comment() {
	}

	Comment(long taskId, long authorId, String content, Instant now) {
		this.taskId = taskId;
		this.authorId = authorId;
		this.content = content;
		this.createdAt = now;
	}

	Long getId() {
		return this.id;
	}

	Long getTaskId() {
		return this.taskId;
	}

	Long getAuthorId() {
		return this.authorId;
	}

	String getContent() {
		return this.content;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

}
