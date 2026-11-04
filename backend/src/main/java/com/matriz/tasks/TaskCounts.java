package com.matriz.tasks;

/**
 * Number of tasks per status in a project (docs/openapi.yaml, TaskCounts).
 */
public record TaskCounts(int todo, int inProgress, int done) {

	public static final TaskCounts NONE = new TaskCounts(0, 0, 0);

}
