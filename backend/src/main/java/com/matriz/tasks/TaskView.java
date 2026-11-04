package com.matriz.tasks;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A task (docs/openapi.yaml, Task). {@code late} is worked out, not stored (FR-08).
 */
public record TaskView(long id, long projectId, String title, String description, List<UserSummary> assignees,
		LocalDate dueDate, Priority priority, TaskStatus status, boolean late, Instant createdAt, Instant updatedAt) {

}
