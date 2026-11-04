package com.matriz.tasks;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.accounts.AuthenticatedUser;

/**
 * A task's comments (US-11).
 */
@RestController
@RequestMapping("/projects/{projectId}/tasks/{taskId}/comments")
class CommentController {

	private final CommentService commentService;

	CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@GetMapping
	List<CommentView> listComments(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId, @PathVariable @Positive long taskId) {
		return this.commentService.listComments(caller.id(), projectId, taskId);
	}

	@PostMapping
	ResponseEntity<CommentView> addComment(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId, @PathVariable @Positive long taskId,
			@Valid @RequestBody CommentRequest body) {
		CommentView comment = this.commentService.addComment(caller.id(), projectId, taskId, body.content());
		return ResponseEntity.status(HttpStatus.CREATED).body(comment);
	}

}
