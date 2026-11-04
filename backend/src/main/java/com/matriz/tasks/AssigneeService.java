package com.matriz.tasks;

import java.time.Clock;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.accounts.AccountService;
import com.matriz.common.web.FieldValidationException;
import com.matriz.projects.ProjectService;

/**
 * Task assignees (US-10, EC-08, EC-09). Any project member can assign and unassign project
 * members (docs/constraints.md, section 2).
 */
@Service
public class AssigneeService {

	private final TaskService taskService;

	private final TaskAssigneeRepository assignees;

	private final ProjectService projects;

	private final AccountService accounts;

	private final Clock clock;

	AssigneeService(TaskService taskService, TaskAssigneeRepository assignees, ProjectService projects,
			AccountService accounts, Optional<Clock> clock) {
		this.taskService = taskService;
		this.assignees = assignees;
		this.projects = projects;
		this.accounts = accounts;
		this.clock = clock.orElseGet(Clock::systemUTC);
	}

	/**
	 * Assigns a project member to the task, and returns the task with its assignees.
	 */
	@Transactional
	public TaskView assign(long callerId, long projectId, long taskId, long userId) {
		Task task = this.taskService.requireTaskForMember(callerId, projectId, taskId);
		if (this.accounts.findUser(userId).isEmpty()) {
			throw new FieldValidationException("userId", "USER_NOT_FOUND", "No account found for this user.");
		}
		if (this.projects.findRole(userId, projectId).isEmpty()) {
			throw new FieldValidationException("userId", "NOT_A_MEMBER", "Only project members can be assigned.");
		}
		if (this.assignees.existsById(new TaskAssignee.Key(taskId, userId))) {
			throw TaskErrors.alreadyAssigned();
		}
		try {
			this.assignees.saveAndFlush(new TaskAssignee(taskId, userId, this.clock.instant()));
		}
		catch (DataIntegrityViolationException ex) {
			// Assigned at the same moment by another request: the primary key keeps one row.
			throw TaskErrors.alreadyAssigned();
		}
		return this.taskService.view(task);
	}

	@Transactional
	public void unassign(long callerId, long projectId, long taskId, long userId) {
		this.taskService.requireTaskForMember(callerId, projectId, taskId);
		TaskAssignee assignment = this.assignees.findById(new TaskAssignee.Key(taskId, userId))
			.orElseThrow(TaskErrors::assignmentNotFound);
		this.assignees.delete(assignment);
	}

}
