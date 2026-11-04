package com.matriz.tasks;

import java.time.Instant;

/**
 * A comment (docs/openapi.yaml, Comment). {@code author} is null if their account was deleted.
 */
public record CommentView(long id, long taskId, String content, UserSummary author, Instant createdAt) {

}
