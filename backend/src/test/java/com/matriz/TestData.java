package com.matriz;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.matriz.accounts.AuthenticatedUser;

/**
 * Creates test users and projects with SQL, so tests don't depend on or change the demo data.
 * Everything it creates has an email or name starting with "test-" and is removed by
 * {@link #deleteAll()}.
 */
public final class TestData {

	private final JdbcTemplate jdbc;

	public TestData(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public TestUser user(String name) {
		String email = "test-" + UUID.randomUUID() + "@example.com";
		Long id = this.jdbc.queryForObject(
				"INSERT INTO app_user (name, email, password_hash) VALUES (?, ?, 'not-a-real-hash') RETURNING id",
				Long.class, name, email);
		return new TestUser(id, name, email);
	}

	/**
	 * A project with the given project manager and members.
	 */
	public long project(String name, TestUser manager, TestUser... members) {
		Long id = this.jdbc.queryForObject(
				"INSERT INTO project (name, created_at, updated_at) VALUES (?, NOW(), NOW()) RETURNING id", Long.class,
				name);
		this.jdbc.update("INSERT INTO project_member (project_id, user_id, role) VALUES (?, ?, 'PROJECT_MANAGER')", id,
				manager.id());
		for (TestUser member : members) {
			this.jdbc.update("INSERT INTO project_member (project_id, user_id, role) VALUES (?, ?, 'MEMBER')", id,
					member.id());
		}
		return id;
	}

	public long task(long projectId, String title, String status, TestUser... assignees) {
		Long id = this.jdbc.queryForObject("""
				INSERT INTO task (project_id, title, status, created_at, updated_at)
				VALUES (?, ?, ?, NOW(), NOW()) RETURNING id""", Long.class, projectId, title, status);
		for (TestUser assignee : assignees) {
			this.jdbc.update("INSERT INTO task_assignee (task_id, user_id, created_at) VALUES (?, ?, NOW())", id,
					assignee.id());
		}
		return id;
	}

	public long task(long projectId, String title, String status, String priority, java.time.LocalDate dueDate) {
		return this.jdbc.queryForObject("""
				INSERT INTO task (project_id, title, status, priority, due_date, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, NOW(), NOW()) RETURNING id""", Long.class, projectId, title, status, priority,
				dueDate);
	}

	public void deleteAll() {
		this.jdbc.update("DELETE FROM project WHERE id IN (SELECT m.project_id FROM project_member m "
				+ "JOIN app_user u ON u.id = m.user_id WHERE u.email LIKE 'test-%')");
		this.jdbc.update("DELETE FROM sign_in_attempt WHERE email LIKE 'test-%'");
		this.jdbc.update("DELETE FROM app_user WHERE email LIKE 'test-%'");
	}

	public record TestUser(long id, String name, String email) {

		/**
		 * Makes a MockMvc request as this user, signed in.
		 */
		public RequestPostProcessor signedIn() {
			return SecurityMockMvcRequestPostProcessors.authentication(UsernamePasswordAuthenticationToken
				.authenticated(new AuthenticatedUser(this.id, this.email), null, List.of()));
		}

	}

}
