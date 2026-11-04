package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Checks that Liquibase creates the schema in docs/database.md and loads the demo data once.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SchemaAndSeedTests {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	SpringLiquibase liquibase;

	@Autowired
	TransactionTemplate transaction;

	@Test
	void createsAllTables() {
		List<String> tables = jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

		assertThat(tables).contains("app_user", "project", "project_member", "task", "task_assignee", "comment",
				"sign_in_attempt", "spring_session", "spring_session_attributes");
	}

	@Test
	void loadsDemoData() {
		assertThat(seedCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(
				"app_user", 3, "project", 3, "project_member", 5, "task", 8, "task_assignee", 9, "comment", 4));
	}

	@Test
	void demoUsersOnlySeeTheirProjects() {
		List<String> aliceProjects = jdbc.queryForList("""
				SELECT p.name FROM project p
				JOIN project_member m ON m.project_id = p.id
				JOIN app_user u ON u.id = m.user_id
				WHERE u.email = 'alice@example.com' ORDER BY p.name""", String.class);

		assertThat(aliceProjects).containsExactly("Mobile launch", "Website redesign");
	}

	@Test
	void runningTheChangelogAgainDoesNotDeleteOrDuplicateData() throws Exception {
		Map<String, Integer> before = seedCounts();

		liquibase.afterPropertiesSet();

		assertThat(seedCounts()).isEqualTo(before);
	}

	@Test
	void rejectsADuplicateAssignment() {
		assertThatThrownBy(() -> inRolledBackTransaction("""
				INSERT INTO task_assignee (task_id, user_id, created_at)
				SELECT task_id, user_id, NOW() FROM task_assignee LIMIT 1"""))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAnEmailThatDiffersOnlyInCase() {
		assertThatThrownBy(() -> inRolledBackTransaction(
				"INSERT INTO app_user (name, email, password_hash) VALUES ('Alice Again', 'ALICE@example.com', 'x')"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsASecondProjectManager() {
		assertThatThrownBy(() -> inRolledBackTransaction("""
				UPDATE project_member SET role = 'PROJECT_MANAGER'
				WHERE role = 'MEMBER' AND project_id = (SELECT id FROM project WHERE name = 'Website redesign')"""))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsATitleMadeOnlyOfSpaces() {
		assertThatThrownBy(() -> inRolledBackTransaction("""
				INSERT INTO task (project_id, title, created_at, updated_at)
				SELECT id, '     ', NOW(), NOW() FROM project LIMIT 1"""))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Map<String, Integer> seedCounts() {
		return Map.of(
				"app_user", count("app_user"),
				"project", count("project"),
				"project_member", count("project_member"),
				"task", count("task"),
				"task_assignee", count("task_assignee"),
				"comment", count("comment"));
	}

	private int count(String table) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
	}

	private void inRolledBackTransaction(String sql) {
		transaction.executeWithoutResult(status -> {
			status.setRollbackOnly();
			jdbc.update(sql);
		});
	}

}
