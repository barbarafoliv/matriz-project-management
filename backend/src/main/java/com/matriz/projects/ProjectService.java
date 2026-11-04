package com.matriz.projects;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.accounts.AccountService;
import com.matriz.accounts.User;
import com.matriz.common.web.Text;
import com.matriz.common.web.FieldValidationException;
import com.matriz.tasks.ProjectTasks;
import com.matriz.tasks.TaskCounts;

/**
 * Projects and their members (US-02, US-03, US-12).
 *
 * <p>Access rules (docs/database.md, section 4): only members see a project, and a
 * project the caller isn't a member of is reported as not found (EC-04). Only the project
 * manager edits or deletes the project and manages its members.
 */
@Service
public class ProjectService {

	private final ProjectRepository projects;

	private final ProjectMemberRepository members;

	private final AccountService accounts;

	private final ProjectTasks projectTasks;

	private final Clock clock;

	ProjectService(ProjectRepository projects, ProjectMemberRepository members, AccountService accounts,
			ProjectTasks projectTasks, Optional<Clock> clock) {
		this.projects = projects;
		this.members = members;
		this.accounts = accounts;
		this.projectTasks = projectTasks;
		this.clock = clock.orElseGet(Clock::systemUTC);
	}

	/**
	 * The caller's projects, sorted by name. With a search text, only projects whose name
	 * contains it, ignoring case (US-12).
	 */
	@Transactional(readOnly = true)
	public List<ProjectView> listProjects(long callerId, String search) {
		String text = (search == null) ? "" : Text.trim(search);
		List<Project> found = text.isEmpty() ? this.projects.findForMember(callerId)
				: this.projects.searchForMember(callerId, "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%");
		Map<Long, Role> roles = this.members.findByUserId(callerId)
			.stream()
			.collect(Collectors.toMap(ProjectMember::getProjectId, ProjectMember::getRole));
		Map<Long, TaskCounts> counts = this.projectTasks.countByStatus(found.stream().map(Project::getId).toList());
		return found.stream()
			.map(project -> view(project, roles.get(project.getId()), counts.get(project.getId())))
			.toList();
	}

	@Transactional(readOnly = true)
	public ProjectView getProject(long callerId, long projectId) {
		ProjectMember membership = requireMember(callerId, projectId);
		return view(requireProject(projectId), membership.getRole());
	}

	/**
	 * Creates a project. The caller becomes its project manager (US-02).
	 */
	@Transactional
	public ProjectView createProject(long callerId, String name, String description) {
		Project project = this.projects.save(new Project(Text.trim(name), clean(description), this.clock.instant()));
		this.members.save(new ProjectMember(project.getId(), callerId, Role.PROJECT_MANAGER));
		return view(project, Role.PROJECT_MANAGER, TaskCounts.NONE);
	}

	@Transactional
	public ProjectView updateProject(long callerId, long projectId, String name, String description) {
		requireManager(callerId, projectId);
		Project project = requireProject(projectId);
		project.change(Text.trim(name), clean(description), this.clock.instant());
		this.projects.flush();
		return view(project, Role.PROJECT_MANAGER);
	}

	/**
	 * Deletes the project. The database deletes its tasks, their assignments and comments,
	 * and its memberships (US-02).
	 */
	@Transactional
	public void deleteProject(long callerId, long projectId) {
		requireManager(callerId, projectId);
		this.projects.deleteById(projectId);
	}

	/**
	 * All members, including the project manager, sorted by name (US-03). Also the users who
	 * can be assigned to the project's tasks (US-10).
	 */
	@Transactional(readOnly = true)
	public List<MemberView> listMembers(long callerId, long projectId) {
		requireMember(callerId, projectId);
		List<ProjectMember> memberships = this.members.findByProjectId(projectId);
		Map<Long, User> users = this.accounts.findUsers(memberships.stream().map(ProjectMember::getUserId).toList())
			.stream()
			.collect(Collectors.toMap(User::getId, Function.identity()));
		return memberships.stream()
			.filter(membership -> users.containsKey(membership.getUserId()))
			.map(membership -> new MemberView(users.get(membership.getUserId()), membership.getRole()))
			.sorted(Comparator.comparing((MemberView member) -> member.name().toLowerCase(Locale.ROOT))
				.thenComparing(MemberView::userId))
			.toList();
	}

	/**
	 * Adds a registered user by email, as a member (US-03).
	 */
	@Transactional
	public MemberView addMember(long callerId, long projectId, String email) {
		requireManager(callerId, projectId);
		User user = this.accounts.findUserByEmail(email)
			.orElseThrow(() -> new FieldValidationException("email", "USER_NOT_FOUND",
					"No account found with this email."));
		if (this.members.findByProjectIdAndUserId(projectId, user.getId()).isPresent()) {
			throw ProjectErrors.alreadyMember();
		}
		try {
			this.members.saveAndFlush(new ProjectMember(projectId, user.getId(), Role.MEMBER));
		}
		catch (DataIntegrityViolationException ex) {
			// Added at the same moment by another request: the primary key keeps one row.
			throw ProjectErrors.alreadyMember();
		}
		return new MemberView(user, Role.MEMBER);
	}

	/**
	 * Removes a member and their assignments in this project (US-03). The project manager
	 * can't be removed (EC-07).
	 */
	@Transactional
	public void removeMember(long callerId, long projectId, long userId) {
		requireManager(callerId, projectId);
		ProjectMember membership = this.members.findByProjectIdAndUserId(projectId, userId)
			.orElseThrow(ProjectErrors::memberNotFound);
		if (membership.isProjectManager()) {
			throw ProjectErrors.projectManagerNotRemovable();
		}
		this.projectTasks.removeAssignee(projectId, userId);
		this.members.delete(membership);
	}

	/**
	 * Whether the user is a member of the project. For other modules, such as Tasks.
	 */
	@Transactional(readOnly = true)
	public Optional<Role> findRole(long userId, long projectId) {
		return this.members.findByProjectIdAndUserId(projectId, userId).map(ProjectMember::getRole);
	}

	/**
	 * The names of the projects the user is a member of, by project ID. For other modules,
	 * such as "My tasks" (US-06).
	 */
	@Transactional(readOnly = true)
	public Map<Long, String> projectNamesOf(long userId) {
		return this.projects.findForMember(userId)
			.stream()
			.collect(Collectors.toMap(Project::getId, Project::getName));
	}

	/**
	 * Whether the user is the project manager of any project. Their account can't be deleted
	 * until those projects are deleted (EC-06).
	 */
	@Transactional(readOnly = true)
	public boolean managesAnyProject(long userId) {
		return this.members.findByUserId(userId).stream().anyMatch(ProjectMember::isProjectManager);
	}

	private ProjectMember requireMember(long callerId, long projectId) {
		return this.members.findByProjectIdAndUserId(projectId, callerId).orElseThrow(ProjectErrors::projectNotFound);
	}

	private void requireManager(long callerId, long projectId) {
		if (!requireMember(callerId, projectId).isProjectManager()) {
			throw ProjectErrors.notProjectManager();
		}
	}

	private Project requireProject(long projectId) {
		return this.projects.findById(projectId).orElseThrow(ProjectErrors::projectNotFound);
	}

	private ProjectView view(Project project, Role role) {
		TaskCounts counts = this.projectTasks.countByStatus(List.of(project.getId())).get(project.getId());
		return view(project, role, counts);
	}

	private static ProjectView view(Project project, Role role, TaskCounts counts) {
		return new ProjectView(project.getId(), project.getName(), project.getDescription(), role,
				(counts != null) ? counts : TaskCounts.NONE, project.getCreatedAt(), project.getUpdatedAt());
	}

	/**
	 * Trims the description; an empty one is stored as no description.
	 */
	private static String clean(String description) {
		if (Text.isBlank(description)) {
			return null;
		}
		return Text.trim(description);
	}

	private static String escapeLike(String text) {
		return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}

}
