package com.matriz.tasks;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /projects/{projectId}/tasks/{taskId}/status} (docs/openapi.yaml).
 */
record UpdateTaskStatusRequest(@NotNull(message = "Choose a status.") TaskStatus status) {

}
