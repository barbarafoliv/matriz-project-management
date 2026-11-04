package com.matriz.tasks;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.accounts.AuthenticatedUser;

/**
 * "My tasks": the tasks assigned to the signed-in user, in all their projects (US-06).
 */
@RestController
class MyTasksController {

	private final TaskService taskService;

	MyTasksController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping("/users/me/tasks")
	List<MyTask> myTasks(@AuthenticationPrincipal AuthenticatedUser caller) {
		return this.taskService.listMyTasks(caller.id());
	}

}
