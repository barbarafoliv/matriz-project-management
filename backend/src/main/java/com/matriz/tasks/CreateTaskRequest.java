package com.matriz.tasks;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

import com.matriz.common.web.TrimmedLength;

/**
 * Body of {@code POST /projects/{projectId}/tasks} (docs/openapi.yaml, CreateTaskRequest).
 * Without a priority, the task is Medium.
 */
record CreateTaskRequest(
		@NotBlank(message = "Title must not be empty.")
		@TrimmedLength(min = 3, max = 150, message = "Title must be between 3 and 150 characters.") String title,
		@TrimmedLength(max = 2000, message = "Description must be at most 2000 characters.") String description,
		LocalDate dueDate, Priority priority) {

	TaskDetails details() {
		return new TaskDetails(this.title, this.description, this.dueDate, this.priority);
	}

}
