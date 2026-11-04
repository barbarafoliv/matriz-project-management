package com.matriz.projects;

import java.time.Instant;

import com.matriz.tasks.TaskCounts;

/**
 * A project as seen by one member (docs/openapi.yaml, Project).
 */
public record ProjectView(long id, String name, String description, Role myRole, TaskCounts taskCounts,
		Instant createdAt, Instant updatedAt) {

}
