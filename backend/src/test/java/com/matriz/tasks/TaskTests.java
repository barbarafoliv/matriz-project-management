package com.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import com.matriz.TestData;
import com.matriz.TestData.TestUser;
import com.matriz.TestcontainersConfiguration;

/**
 * Tasks: create, list, filter, open, edit, change status and delete, with access rules and
 * the late flag (US-04, US-05, US-07, US-08, US-13, EC-03, EC-04, EC-05, Practical Sheet 03
 * Exercise 5).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TaskTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	JsonMapper json;

	TestData data;

	TestUser ana;

	TestUser ben;

	TestUser cleo;

	long project;

	@BeforeEach
	void createProject() {
		this.data = new TestData(this.jdbc);
		this.ana = this.data.user("Ana Test");
		this.ben = this.data.user("Ben Test");
		this.cleo = this.data.user("Cleo Test");
		this.project = this.data.project("Team project", this.ana, this.ben);
	}

	@AfterEach
	void removeTestData() {
		this.data.deleteAll();
	}

	@Test
	void createsATaskAsToDoWithMediumPriorityByDefault() throws Exception {
		// Ben is a member, not the project manager: any member can create a task (FR-04).
		MvcResult created = this.mvc.perform(asUser(this.ben, post(tasks())).content(body("  Write the copy  ", null, null, null)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", endsWith(tasks() + "/" + newestTaskId())))
			.andExpect(jsonPath("$.id").value(newestTaskId()))
			.andExpect(jsonPath("$.projectId").value(this.project))
			.andExpect(jsonPath("$.title").value("Write the copy"))
			// Optional fields are always present, as null when empty (docs/openapi.yaml).
			.andExpect(jsonPath("$.description").value(nullValue()))
			.andExpect(jsonPath("$.dueDate").value(nullValue()))
			.andExpect(jsonPath("$.assignees", hasSize(0)))
			.andExpect(jsonPath("$.priority").value("MEDIUM"))
			.andExpect(jsonPath("$.status").value("TODO"))
			.andExpect(jsonPath("$.late").value(false))
			.andReturn();
		Map<String, Object> task = this.json.readValue(created.getResponse().getContentAsString(), Map.class);
		assertThat(task).containsOnlyKeys("id", "projectId", "title", "description", "assignees", "dueDate", "priority",
				"status", "late", "createdAt", "updatedAt");
		assertThat(Instant.parse((String) task.get("createdAt"))).isEqualTo(Instant.parse((String) task.get("updatedAt")));

		// Saved: read back from the database ...
		assertThat(this.jdbc.queryForMap(
				"SELECT project_id, title, description, due_date, priority, status FROM task WHERE id = ?", newestTaskId()))
			.containsEntry("project_id", this.project)
			.containsEntry("title", "Write the copy")
			.containsEntry("description", null)
			.containsEntry("due_date", null)
			.containsEntry("priority", "MEDIUM")
			.containsEntry("status", "TODO");

		// ... and through the API, by another member: the same task as the one created.
		String location = created.getResponse().getHeader("Location");
		String read = this.mvc.perform(get(location.substring(location.indexOf("/projects"))).with(this.ana.signedIn()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(this.json.readValue(read, Map.class)).isEqualTo(task);
	}

	@Test
	void createsATaskWithAllItsDetails() throws Exception {
		this.mvc.perform(asUser(this.ana, post(tasks())).content(body("Plan the beta", "Invite ten users.", "2030-01-15", "HIGH")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.description").value("Invite ten users."))
			.andExpect(jsonPath("$.dueDate").value("2030-01-15"))
			.andExpect(jsonPath("$.priority").value("HIGH"));
	}

	@Test
	void rejectsATitleMadeOnlyOfSpacesAndSavesNothing() throws Exception {
		expectFieldError(this.mvc.perform(asUser(this.ana, post(tasks())).content(body("     ", null, null, null))),
				"title", "BLANK");

		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM task WHERE project_id = ?", Integer.class,
				this.project))
			.isOne(); // Temporary: deliberately wrong, to check that CI fails.
	}

	@Test
	void treatsNoBreakSpacesAsSpacesAndSavesTheTitleTrimmed() throws Exception {
		expectFieldError(this.mvc.perform(asUser(this.ana, post(tasks())).content(body("\u00A0\u00A0\u00A0", null, null, null))),
				"title", "BLANK");
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM task WHERE project_id = ?", Integer.class,
				this.project))
			.isZero();

		this.mvc.perform(asUser(this.ana, post(tasks())).content(body("\u00A0 Plan \u00A0", "\u00A0 Details \u2003", null, null)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.title").value("Plan"))
			.andExpect(jsonPath("$.description").value("Details"));
	}

	@Test
	void rejectsOtherInvalidFields() throws Exception {
		expectFieldError(this.mvc.perform(asUser(this.ana, post(tasks())).content("{}")), "title", "REQUIRED");
		expectFieldError(this.mvc.perform(asUser(this.ana, post(tasks())).content(body(" ab ", null, null, null))),
				"title", "TOO_SHORT");
		expectFieldError(this.mvc.perform(asUser(this.ana, post(tasks())).content(body("x".repeat(151), null, null, null))),
				"title", "TOO_LONG");
		expectFieldError(this.mvc.perform(asUser(this.ana, post(tasks())).content(body("Fine", "x".repeat(2001), null, null))),
				"description", "TOO_LONG");
		this.mvc.perform(asUser(this.ana, post(tasks())).content(body("Fine", null, null, "URGENT")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
		this.mvc.perform(asUser(this.ana, post(tasks())).content(body("Fine", null, "15/01/2030", null)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void checksTheDescriptionLengthAfterTrimming() throws Exception {
		this.mvc.perform(asUser(this.ana, post(tasks())).content(body("Fine", "x".repeat(2000) + "   ", null, null)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.description").value("x".repeat(2000)));

		long task = this.data.task(this.project, "To edit", "TODO", "LOW", null);
		this.mvc.perform(asUser(this.ana, put(tasks() + "/" + task)).content(body("To edit", "y".repeat(2000) + "  ", null, "LOW")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.description").value("y".repeat(2000)));
	}

	@Test
	void savesATitleOf150EmojiBecauseLengthsCountCharacters() throws Exception {
		String title = "😀".repeat(150);
		this.mvc.perform(asUser(this.ana, post(tasks())).content(body(title, null, null, null)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.title").value(title));
		assertThat(this.jdbc.queryForObject("SELECT title FROM task WHERE id = ?", String.class, newestTaskId()))
			.isEqualTo(title);

		expectFieldError(this.mvc.perform(asUser(this.ana, post(tasks())).content(body("😀".repeat(151), null, null, null))),
				"title", "TOO_LONG");
		expectFieldError(this.mvc.perform(asUser(this.ana, post(tasks())).content(body("😀".repeat(2), null, null, null))),
				"title", "TOO_SHORT");
	}

	@Test
	void listsTasksByDueDateWithTasksWithoutOneLast() throws Exception {
		this.data.task(this.project, "No date", "TODO", "LOW", null);
		this.data.task(this.project, "Later", "TODO", "LOW", LocalDate.of(2030, 3, 1));
		this.data.task(this.project, "Sooner", "DONE", "LOW", LocalDate.of(2030, 1, 1));

		this.mvc.perform(get(tasks()).with(this.ben.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].title").value(contains("Sooner", "Later", "No date")));
	}

	@Test
	void filtersByStatusAndPriority() throws Exception {
		this.data.task(this.project, "High to do", "TODO", "HIGH", null);
		this.data.task(this.project, "High in progress", "IN_PROGRESS", "HIGH", null);
		this.data.task(this.project, "Low in progress", "IN_PROGRESS", "LOW", null);

		this.mvc.perform(get(tasks()).param("priority", "HIGH").with(this.ana.signedIn()))
			.andExpect(jsonPath("$[*].title").value(contains("High to do", "High in progress")));
		this.mvc.perform(get(tasks()).param("status", "IN_PROGRESS").with(this.ana.signedIn()))
			.andExpect(jsonPath("$[*].title").value(contains("High in progress", "Low in progress")));
		this.mvc.perform(get(tasks()).param("status", "IN_PROGRESS").param("priority", "HIGH").with(this.ana.signedIn()))
			.andExpect(jsonPath("$[*].title").value(contains("High in progress")));
		this.mvc.perform(get(tasks()).param("status", "DONE").with(this.ana.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
		this.mvc.perform(get(tasks()).param("status", "FINISHED").with(this.ana.signedIn()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void marksLateTasksUsingTodaysDate() throws Exception {
		LocalDate today = LocalDate.now();
		long lateTask = this.data.task(this.project, "Due yesterday", "IN_PROGRESS", "LOW", today.minusDays(1));
		long dueToday = this.data.task(this.project, "Due today", "TODO", "LOW", today);
		long doneYesterday = this.data.task(this.project, "Done yesterday", "DONE", "LOW", today.minusDays(1));
		long noDate = this.data.task(this.project, "No date", "TODO", "LOW", null);

		expectLate(lateTask, true);
		expectLate(dueToday, false);
		expectLate(doneYesterday, false);
		expectLate(noDate, false);

		// A late task is no longer late once it is done (US-08).
		this.mvc.perform(asUser(this.ben, put(tasks() + "/" + lateTask + "/status")).content("{\"status\":\"DONE\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.late").value(false));
	}

	@Test
	void changesTheStatusInAnyDirection() throws Exception {
		long task = this.data.task(this.project, "Movable", "TODO", "LOW", null);

		for (String status : new String[] { "IN_PROGRESS", "DONE", "IN_PROGRESS", "TODO" }) {
			this.mvc.perform(asUser(this.ben, put(tasks() + "/" + task + "/status")).content("{\"status\":\"" + status + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(status));
		}
		this.mvc.perform(asUser(this.ben, put(tasks() + "/" + task + "/status")).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("status"));
	}

	@Test
	void anyMemberCanEditATaskAndNullClearsOptionalFields() throws Exception {
		long task = this.data.task(this.project, "Old title", "TODO", "LOW", LocalDate.of(2030, 1, 1));

		this.mvc.perform(asUser(this.ben, put(tasks() + "/" + task)).content(body("New title", "Details", null, "HIGH")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("New title"))
			.andExpect(jsonPath("$.description").value("Details"))
			.andExpect(jsonPath("$.dueDate").doesNotExist())
			.andExpect(jsonPath("$.priority").value("HIGH"))
			.andExpect(jsonPath("$.status").value("TODO"));
		this.mvc.perform(get(tasks() + "/" + task).with(this.ana.signedIn()))
			.andExpect(jsonPath("$.title").value("New title"));

		expectFieldError(this.mvc.perform(asUser(this.ben, put(tasks() + "/" + task)).content(body("New title", null, null, null))),
				"priority", "REQUIRED");
	}

	@Test
	void onlyTheProjectManagerCanDeleteATask() throws Exception {
		long task = this.data.task(this.project, "To delete", "TODO", this.ben);
		this.jdbc.update("INSERT INTO comment (task_id, author_id, content, created_at) VALUES (?, ?, 'Hi', NOW())",
				task, this.ben.id());

		this.mvc.perform(delete(tasks() + "/" + task).with(this.ben.signedIn()).with(csrf()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_PROJECT_MANAGER"));

		this.mvc.perform(delete(tasks() + "/" + task).with(this.ana.signedIn()).with(csrf()))
			.andExpect(status().isNoContent());
		this.mvc.perform(get(tasks() + "/" + task).with(this.ana.signedIn()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
		for (String table : new String[] { "task_assignee", "comment" }) {
			assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE task_id = ?", Integer.class,
					task))
				.as(table)
				.isZero();
		}
	}

	@Test
	void nonMembersCannotSeeOrChangeTasksEvenWhenTheIdsAreChanged() throws Exception {
		long task = this.data.task(this.project, "Private", "TODO", "LOW", null);
		String url = tasks() + "/" + task;

		for (ResultActions result : new ResultActions[] { this.mvc.perform(get(tasks()).with(this.cleo.signedIn())),
				this.mvc.perform(get(url).with(this.cleo.signedIn())),
				this.mvc.perform(asUser(this.cleo, post(tasks())).content(body("Sneaky", null, null, null))),
				this.mvc.perform(asUser(this.cleo, put(url)).content(body("Taken", null, null, "LOW"))),
				this.mvc.perform(asUser(this.cleo, put(url + "/status")).content("{\"status\":\"DONE\"}")),
				this.mvc.perform(delete(url).with(this.cleo.signedIn()).with(csrf())) }) {
			result.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
		}
		assertThat(this.jdbc.queryForObject("SELECT title || ' ' || status FROM task WHERE id = ?", String.class, task))
			.isEqualTo("Private TODO");
	}

	@Test
	void requestsWithoutASessionAreRefusedAndChangeNothing() throws Exception {
		long task = this.data.task(this.project, "Private", "TODO", "LOW", null);
		String url = tasks() + "/" + task;

		// With a valid CSRF token, so only the missing session is checked (NFR-02).
		for (ResultActions result : new ResultActions[] { this.mvc.perform(get(tasks())),
				this.mvc.perform(get(url)),
				this.mvc.perform(post(tasks()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
					.content(body("Sneaky", null, null, null))),
				this.mvc.perform(put(url).with(csrf()).contentType(MediaType.APPLICATION_JSON)
					.content(body("Taken", null, null, "LOW"))),
				this.mvc.perform(put(url + "/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
					.content("{\"status\":\"DONE\"}")),
				this.mvc.perform(delete(url).with(csrf())) }) {
			result.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		}
		assertThat(this.jdbc.queryForObject("SELECT title || ' ' || status FROM task WHERE id = ?", String.class, task))
			.isEqualTo("Private TODO");
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM task WHERE project_id = ?", Integer.class,
				this.project))
			.isEqualTo(1);
	}

	@Test
	void aTaskCanOnlyBeReachedThroughItsOwnProject() throws Exception {
		long task = this.data.task(this.project, "In team project", "TODO", "LOW", null);
		long anasOtherProject = this.data.project("Ana's other project", this.ana);

		this.mvc.perform(get("/projects/" + anasOtherProject + "/tasks/" + task).with(this.ana.signedIn()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
		this.mvc.perform(asUser(this.ana, put("/projects/" + anasOtherProject + "/tasks/" + task + "/status"))
				.content("{\"status\":\"DONE\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	void reportsATaskThatDoesNotExistAsNotFound() throws Exception {
		long missing = this.jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000 FROM task", Long.class);
		String url = tasks() + "/" + missing;

		this.mvc.perform(get(url).with(this.ana.signedIn()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
		this.mvc.perform(asUser(this.ana, put(url)).content(body("Ghost", null, null, "LOW")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM task WHERE project_id = ?", Integer.class,
				this.project))
			.isZero();
	}

	@Test
	void showsTheAssigneesSortedByName() throws Exception {
		long task = this.data.task(this.project, "Shared", "TODO", this.ben, this.ana);

		this.mvc.perform(get(tasks() + "/" + task).with(this.ana.signedIn()))
			.andExpect(jsonPath("$.assignees[*].name").value(contains("Ana Test", "Ben Test")))
			.andExpect(jsonPath("$.assignees[0].id").value(this.ana.id()));
	}

	@Test
	void projectCountsFollowStatusChanges() throws Exception {
		long task = this.data.task(this.project, "Counted", "TODO", "LOW", null);

		this.mvc.perform(asUser(this.ana, put(tasks() + "/" + task + "/status")).content("{\"status\":\"DONE\"}"));

		this.mvc.perform(get("/projects/" + this.project).with(this.ana.signedIn()))
			.andExpect(jsonPath("$.taskCounts.todo").value(0))
			.andExpect(jsonPath("$.taskCounts.done").value(1));
	}

	private String tasks() {
		return "/projects/" + this.project + "/tasks";
	}

	private static MockHttpServletRequestBuilder asUser(TestUser user, MockHttpServletRequestBuilder request) {
		return request.with(user.signedIn()).with(csrf()).contentType(MediaType.APPLICATION_JSON);
	}

	private String body(String title, String description, String dueDate, String priority) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		body.put("description", description);
		body.put("dueDate", dueDate);
		body.put("priority", priority);
		return this.json.writeValueAsString(body);
	}

	private long newestTaskId() {
		return this.jdbc.queryForObject("SELECT MAX(id) FROM task WHERE project_id = ?", Long.class, this.project);
	}

	private void expectLate(long task, boolean late) throws Exception {
		this.mvc.perform(get(tasks() + "/" + task).with(this.ana.signedIn())).andExpect(jsonPath("$.late").value(late));
	}

	private static void expectFieldError(ResultActions result, String field, String code) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors", hasSize(1)))
			.andExpect(jsonPath("$.errors[0].field").value(field))
			.andExpect(jsonPath("$.errors[0].code").value(code));
	}

}
