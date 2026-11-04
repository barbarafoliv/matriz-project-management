package com.matriz.tasks;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code POST .../tasks/{taskId}/assignees} (docs/openapi.yaml, AddAssigneeRequest).
 */
record AddAssigneeRequest(@NotNull(message = "Choose a member.") Long userId) {

}
