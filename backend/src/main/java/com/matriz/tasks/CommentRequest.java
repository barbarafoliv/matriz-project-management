package com.matriz.tasks;

import jakarta.validation.constraints.NotBlank;

import com.matriz.common.web.TrimmedLength;

/**
 * Body of {@code POST .../tasks/{taskId}/comments} (docs/openapi.yaml, CommentRequest).
 * The author and date are not part of it: the server sets them.
 */
record CommentRequest(
		@NotBlank(message = "Comment must not be empty.")
		@TrimmedLength(min = 1, max = 2000, message = "Comment must be at most 2000 characters.") String content) {

}
