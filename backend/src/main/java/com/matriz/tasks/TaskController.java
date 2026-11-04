package com.matriz.tasks;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.matriz.accounts.AuthenticatedUser;

/**
 * A project's tasks (US-04, US-05, US-07, US-08, US-13). The caller is always taken from
 * the session.
 */
@RestController
@RequestMapping("/projects/{projectId}/tasks")
class TaskController {

	private final TaskService taskService;

	TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping
	List<TaskView> listTasks(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable @Positive long projectId,
			@RequestParam(required = false) TaskStatus status, @RequestParam(required = false) Priority priority) {
		return this.taskService.listTasks(caller.id(), projectId, status, priority);
	}

	@PostMapping
	ResponseEntity<TaskView> createTask(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId, @Valid @RequestBody CreateTaskRequest body) {
		TaskView task = this.taskService.createTask(caller.id(), projectId, body.details());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{taskId}")
			.buildAndExpand(task.id())
			.toUri();
		return ResponseEntity.created(location).body(task);
	}

	@GetMapping("/{taskId}")
	TaskView getTask(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable @Positive long projectId,
			@PathVariable @Positive long taskId) {
		return this.taskService.getTask(caller.id(), projectId, taskId);
	}

	@PutMapping("/{taskId}")
	TaskView updateTask(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable @Positive long projectId,
			@PathVariable @Positive long taskId, @Valid @RequestBody UpdateTaskRequest body) {
		return this.taskService.updateTask(caller.id(), projectId, taskId, body.details());
	}

	@PutMapping("/{taskId}/status")
	TaskView changeStatus(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable @Positive long projectId,
			@PathVariable @Positive long taskId, @Valid @RequestBody UpdateTaskStatusRequest body) {
		return this.taskService.changeStatus(caller.id(), projectId, taskId, body.status());
	}

	@DeleteMapping("/{taskId}")
	ResponseEntity<Void> deleteTask(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable @Positive long projectId, @PathVariable @Positive long taskId) {
		this.taskService.deleteTask(caller.id(), projectId, taskId);
		return ResponseEntity.noContent().build();
	}

}
