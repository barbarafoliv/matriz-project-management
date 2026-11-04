package com.matriz.tasks;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.accounts.AuthenticatedUser;

/**
 * Task assignees (US-10).
 */
@RestController
@RequestMapping("/projects/{projectId}/tasks/{taskId}/assignees")
class AssigneeController {

	private final AssigneeService assigneeService;

	AssigneeController(AssigneeService assigneeService) {
		this.assigneeService = assigneeService;
	}

	@PostMapping
	ResponseEntity<TaskView> assign(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId, @PathVariable @Positive long taskId,
			@Valid @RequestBody AddAssigneeRequest body) {
		TaskView task = this.assigneeService.assign(caller.id(), projectId, taskId, body.userId());
		return ResponseEntity.status(HttpStatus.CREATED).body(task);
	}

	@DeleteMapping("/{userId}")
	ResponseEntity<Void> unassign(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId, @PathVariable @Positive long taskId,
			@PathVariable @Positive long userId) {
		this.assigneeService.unassign(caller.id(), projectId, taskId, userId);
		return ResponseEntity.noContent().build();
	}

}
