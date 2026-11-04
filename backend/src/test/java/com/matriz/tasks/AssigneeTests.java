package com.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
import org.springframework.test.web.servlet.ResultActions;

import com.matriz.TestData;
import com.matriz.TestData.TestUser;
import com.matriz.TestcontainersConfiguration;

/**
 * Assignees and "My tasks" (US-10, US-06, EC-08, EC-09, Practical Sheet 03 Exercise 6).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AssigneeTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	TestData data;

	TestUser ana;

	TestUser ben;

	TestUser cleo;

	long project;

	long task;

	@BeforeEach
	void createTask() {
		this.data = new TestData(this.jdbc);
		this.ana = this.data.user("Ana Test");
		this.ben = this.data.user("Ben Test");
		this.cleo = this.data.user("Cleo Test");
		this.project = this.data.project("Team project", this.ana, this.ben);
		this.task = this.data.task(this.project, "Shared work", "TODO");
	}

	@AfterEach
	void removeTestData() {
		this.data.deleteAll();
	}

	@Test
	void anyMemberCanAssignSeveralMembers() throws Exception {
		assign(this.ben, this.ana.id()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.assignees[*].name").value(contains("Ana Test")));
		assign(this.ben, this.ben.id()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.assignees[*].name").value(contains("Ana Test", "Ben Test")));

		// Still there when the task is opened again.
		this.mvc.perform(get(assignees().replace("/assignees", "")).with(this.ana.signedIn()))
			.andExpect(jsonPath("$.assignees[*].id").value(contains((int) this.ana.id(), (int) this.ben.id())));
	}

	@Test
	void doesNotAssignTheSameMemberTwice() throws Exception {
		assign(this.ana, this.ben.id()).andExpect(status().isCreated());

		assign(this.ana, this.ben.id()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_ASSIGNED"));
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM task_assignee WHERE task_id = ?", Integer.class,
				this.task))
			.isEqualTo(1);
	}

	@Test
	void onlyAssignsExistingProjectMembers() throws Exception {
		assign(this.ana, this.cleo.id()).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("userId"))
			.andExpect(jsonPath("$.errors[0].code").value("NOT_A_MEMBER"));
		assign(this.ana, 999_999_999L).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].code").value("USER_NOT_FOUND"));
		this.mvc.perform(post(assignees()).with(this.ana.signedIn()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM task_assignee WHERE task_id = ?", Integer.class,
				this.task))
			.isZero();
	}

	@Test
	void removesEachAssignment() throws Exception {
		assign(this.ana, this.ana.id());
		assign(this.ana, this.ben.id());

		this.mvc.perform(delete(assignees() + "/" + this.ana.id()).with(this.ben.signedIn()).with(csrf()))
			.andExpect(status().isNoContent());
		this.mvc.perform(get(assignees().replace("/assignees", "")).with(this.ana.signedIn()))
			.andExpect(jsonPath("$.assignees[*].name").value(contains("Ben Test")));

		this.mvc.perform(delete(assignees() + "/" + this.ana.id()).with(this.ben.signedIn()).with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ASSIGNMENT_NOT_FOUND"));
	}

	@Test
	void nonMembersCannotAssignOrUnassignEvenWhenTheIdsAreChanged() throws Exception {
		assign(this.ana, this.ben.id());

		assign(this.cleo, this.cleo.id()).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
		this.mvc.perform(delete(assignees() + "/" + this.ben.id()).with(this.cleo.signedIn()).with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

		// A task of another project, through a project the caller belongs to.
		long cleosProject = this.data.project("Cleo's project", this.cleo);
		this.mvc.perform(post("/projects/" + cleosProject + "/tasks/" + this.task + "/assignees")
				.with(this.cleo.signedIn()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"userId\":" + this.cleo.id() + "}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM task_assignee WHERE task_id = ?", Integer.class,
				this.task))
			.isEqualTo(1);
	}

	@Test
	void myTasksListsTheTasksAssignedToMeInAllMyProjects() throws Exception {
		long otherProject = this.data.project("Other project", this.ben, this.ana);
		long later = this.data.task(otherProject, "Later task", "IN_PROGRESS", "HIGH", LocalDate.of(2030, 6, 1));
		long sooner = this.data.task(this.project, "Sooner task", "TODO", "LOW", LocalDate.of(2030, 1, 1));
		long noDate = this.data.task(this.project, "No date task", "TODO", "LOW", null);
		long notMine = this.data.task(this.project, "Ben's task", "TODO", "LOW", LocalDate.of(2029, 1, 1));
		for (long id : new long[] { later, sooner, noDate }) {
			this.jdbc.update("INSERT INTO task_assignee (task_id, user_id, created_at) VALUES (?, ?, NOW())", id,
					this.ana.id());
		}
		this.jdbc.update("INSERT INTO task_assignee (task_id, user_id, created_at) VALUES (?, ?, NOW())", notMine,
				this.ben.id());
		this.jdbc.update("INSERT INTO task_assignee (task_id, user_id, created_at) VALUES (?, ?, NOW())", later,
				this.ben.id());

		this.mvc.perform(get("/users/me/tasks").with(this.ana.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].title").value(contains("Sooner task", "Later task", "No date task")))
			.andExpect(jsonPath("$[1].project.name").value("Other project"))
			.andExpect(jsonPath("$[1].project.id").value(otherProject))
			.andExpect(jsonPath("$[1].priority").value("HIGH"))
			.andExpect(jsonPath("$[1].assignees", hasSize(2)))
			.andExpect(jsonPath("$[0].late").value(false));
	}

	@Test
	void myTasksIsEmptyWithoutAssignments() throws Exception {
		this.mvc.perform(get("/users/me/tasks").with(this.cleo.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void removedMembersLoseTheirTasksFromMyTasks() throws Exception {
		assign(this.ana, this.ben.id());

		this.mvc.perform(delete("/projects/" + this.project + "/members/" + this.ben.id()).with(this.ana.signedIn())
				.with(csrf()))
			.andExpect(status().isNoContent());

		this.mvc.perform(get("/users/me/tasks").with(this.ben.signedIn())).andExpect(jsonPath("$", hasSize(0)));
	}

	private String assignees() {
		return "/projects/" + this.project + "/tasks/" + this.task + "/assignees";
	}

	private ResultActions assign(TestUser caller, long userId) throws Exception {
		return this.mvc.perform(post(assignees()).with(caller.signedIn())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"userId\":" + userId + "}"));
	}

}
