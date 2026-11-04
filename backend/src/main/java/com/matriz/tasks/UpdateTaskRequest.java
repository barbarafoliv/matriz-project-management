package com.matriz.tasks;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.matriz.common.web.TrimmedLength;

/**
 * Body of {@code PUT /projects/{projectId}/tasks/{taskId}} (docs/openapi.yaml,
 * UpdateTaskRequest). Full replacement: a null description or due date clears it.
 */
record UpdateTaskRequest(
		@NotBlank(message = "Title must not be empty.")
		@TrimmedLength(min = 3, max = 150, message = "Title must be between 3 and 150 characters.") String title,
		@Size(max = 2000, message = "Description must be at most 2000 characters.") String description,
		LocalDate dueDate, @NotNull(message = "Choose a priority.") Priority priority) {

	TaskDetails details() {
		return new TaskDetails(this.title, this.description, this.dueDate, this.priority);
	}

}
