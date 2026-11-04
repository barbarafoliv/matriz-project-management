package com.matriz.tasks;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.accounts.AccountService;
import com.matriz.accounts.User;
import com.matriz.projects.ProjectService;
import com.matriz.projects.Role;

/**
 * Tasks of a project (US-04, US-05, US-07, US-08, US-13).
 *
 * <p>Access rules (docs/database.md, section 4): only project members see and change a
 * project's tasks; a project the caller isn't a member of is reported as not found (EC-04).
 * Any member can create and edit tasks and change their status. Only the project manager
 * deletes them.
 */
@Service
public class TaskService {

	/** Board order: by due date, tasks without one last (docs/openapi.yaml). */
	private static final Sort BY_DUE_DATE = Sort.by(Sort.Order.asc("dueDate").nullsLast(), Sort.Order.asc("id"));

	private final TaskRepository tasks;

	private final TaskAssigneeRepository assignees;

	private final ProjectService projects;

	private final AccountService accounts;

	private final Clock clock;

	TaskService(TaskRepository tasks, TaskAssigneeRepository assignees, ProjectService projects,
			AccountService accounts, Optional<Clock> clock) {
		this.tasks = tasks;
		this.assignees = assignees;
		this.projects = projects;
		this.accounts = accounts;
		// "Late" uses the local date of the machine running the application (ADR 0006).
		this.clock = clock.orElseGet(Clock::systemDefaultZone);
	}

	/**
	 * The project's tasks, optionally only those with the given status and priority (US-13).
	 */
	@Transactional(readOnly = true)
	public List<TaskView> listTasks(long callerId, long projectId, TaskStatus status, Priority priority) {
		requireMember(callerId, projectId);
		List<Task> found = this.tasks.findAll(Specification.allOf(TaskRepository.inProject(projectId),
				TaskRepository.withStatus(status), TaskRepository.withPriority(priority)), BY_DUE_DATE);
		return views(found);
	}

	@Transactional(readOnly = true)
	public TaskView getTask(long callerId, long projectId, long taskId) {
		requireMember(callerId, projectId);
		return view(requireTask(projectId, taskId));
	}

	/**
	 * Creates a task with status To Do and, unless another is given, priority Medium (US-04).
	 */
	@Transactional
	public TaskView createTask(long callerId, long projectId, TaskDetails details) {
		requireMember(callerId, projectId);
		Priority priority = (details.priority() != null) ? details.priority() : Priority.MEDIUM;
		Task task = this.tasks.save(new Task(projectId, details.title().strip(), clean(details.description()),
				details.dueDate(), priority, this.clock.instant()));
		return view(task);
	}

	/**
	 * Replaces the task's details (US-04). Any member can edit.
	 */
	@Transactional
	public TaskView updateTask(long callerId, long projectId, long taskId, TaskDetails details) {
		requireMember(callerId, projectId);
		Task task = requireTask(projectId, taskId);
		task.change(details.title().strip(), clean(details.description()), details.dueDate(), details.priority(),
				this.clock.instant());
		this.tasks.flush();
		return view(task);
	}

	/**
	 * Moves the task to any status, in any direction (US-05, US-07).
	 */
	@Transactional
	public TaskView changeStatus(long callerId, long projectId, long taskId, TaskStatus status) {
		requireMember(callerId, projectId);
		Task task = requireTask(projectId, taskId);
		task.changeStatus(status, this.clock.instant());
		this.tasks.flush();
		return view(task);
	}

	/**
	 * Deletes the task. The database deletes its assignments and comments (US-04).
	 */
	@Transactional
	public void deleteTask(long callerId, long projectId, long taskId) {
		if (requireMember(callerId, projectId) != Role.PROJECT_MANAGER) {
			throw TaskErrors.notProjectManager();
		}
		this.tasks.delete(requireTask(projectId, taskId));
	}

	private Role requireMember(long callerId, long projectId) {
		return this.projects.findRole(callerId, projectId).orElseThrow(TaskErrors::projectNotFound);
	}

	/**
	 * A task of another project is reported as not found, even if the caller can see that
	 * other project.
	 */
	private Task requireTask(long projectId, long taskId) {
		return this.tasks.findByIdAndProjectId(taskId, projectId).orElseThrow(TaskErrors::taskNotFound);
	}

	private TaskView view(Task task) {
		return views(List.of(task)).get(0);
	}

	private List<TaskView> views(List<Task> found) {
		Map<Long, List<UserSummary>> assigneesByTask = assigneesOf(found.stream().map(Task::getId).toList());
		LocalDate today = LocalDate.now(this.clock);
		return found.stream()
			.map(task -> new TaskView(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(),
					assigneesByTask.getOrDefault(task.getId(), List.of()), task.getDueDate(), task.getPriority(),
					task.getStatus(), task.isLateOn(today), task.getCreatedAt(), task.getUpdatedAt()))
			.toList();
	}

	/**
	 * The assignees of each task, sorted by name (docs/openapi.yaml, Task).
	 */
	private Map<Long, List<UserSummary>> assigneesOf(Collection<Long> taskIds) {
		if (taskIds.isEmpty()) {
			return Map.of();
		}
		List<TaskAssignee> rows = this.assignees.findByTaskIdIn(taskIds);
		Map<Long, User> users = this.accounts.findUsers(rows.stream().map(TaskAssignee::getUserId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(User::getId, Function.identity()));
		Comparator<UserSummary> byName = Comparator.comparing((UserSummary user) -> user.name().toLowerCase(Locale.ROOT))
			.thenComparing(UserSummary::id);
		return rows.stream()
			.filter(row -> users.containsKey(row.getUserId()))
			.collect(Collectors.groupingBy(TaskAssignee::getTaskId,
					Collectors.mapping(row -> UserSummary.of(users.get(row.getUserId())),
							Collectors.collectingAndThen(Collectors.toList(),
									list -> list.stream().sorted(byName).toList()))));
	}

	/**
	 * Trims the description; an empty one is stored as no description.
	 */
	private static String clean(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.strip();
	}

}
