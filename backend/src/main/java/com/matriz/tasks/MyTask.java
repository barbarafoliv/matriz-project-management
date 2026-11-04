package com.matriz.tasks;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A task in "My tasks": a {@link TaskView} plus its project (docs/openapi.yaml, MyTask).
 */
public record MyTask(long id, long projectId, String title, String description, List<UserSummary> assignees,
		LocalDate dueDate, Priority priority, TaskStatus status, boolean late, Instant createdAt, Instant updatedAt,
		ProjectSummary project) {

	static MyTask of(TaskView task, String projectName) {
		return new MyTask(task.id(), task.projectId(), task.title(), task.description(), task.assignees(),
				task.dueDate(), task.priority(), task.status(), task.late(), task.createdAt(), task.updatedAt(),
				new ProjectSummary(task.projectId(), projectName));
	}

	/** docs/openapi.yaml, ProjectSummary. */
	public record ProjectSummary(long id, String name) {
	}

}
