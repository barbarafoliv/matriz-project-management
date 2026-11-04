package com.matriz.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Project members: list, add and remove (US-03, EC-07).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MemberTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

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
	void listsAllMembersSortedByName() throws Exception {
		this.mvc.perform(get(members()).with(this.ben.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(contains("Ana Test", "Ben Test")))
			.andExpect(jsonPath("$[0].role").value("PROJECT_MANAGER"))
			.andExpect(jsonPath("$[0].email").value(this.ana.email()))
			.andExpect(jsonPath("$[1].role").value("MEMBER"));
	}

	@Test
	void addsARegisteredUserByEmailIgnoringCase() throws Exception {
		addMember(this.ana, this.cleo.email().toUpperCase())
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", endsWith("/members/" + this.cleo.id())))
			.andExpect(jsonPath("$.userId").value(this.cleo.id()))
			.andExpect(jsonPath("$.role").value("MEMBER"));

		this.mvc.perform(get("/projects").with(this.cleo.signedIn()))
			.andExpect(jsonPath("$[*].name").value(contains("Team project")));
	}

	@Test
	void rejectsAnEmailWithoutAnAccount() throws Exception {
		addMember(this.ana, "test-nobody@example.com")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].field").value("email"))
			.andExpect(jsonPath("$.errors[0].code").value("USER_NOT_FOUND"))
			.andExpect(jsonPath("$.errors[0].message").value("No account found with this email."));
	}

	@Test
	void rejectsSomeoneWhoIsAlreadyAMember() throws Exception {
		addMember(this.ana, this.ben.email())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_MEMBER"));
	}

	@Test
	void simultaneousRequestsToAddTheSameMemberCreateOneMembershipAndNoServerError() throws Exception {
		java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(8);
		java.util.List<java.util.concurrent.Future<Integer>> results = new java.util.ArrayList<>();
		for (int i = 0; i < 8; i++) {
			results.add(pool.submit(() -> addMember(this.ana, this.cleo.email()).andReturn().getResponse().getStatus()));
		}
		java.util.List<Integer> statuses = new java.util.ArrayList<>();
		for (java.util.concurrent.Future<Integer> result : results) {
			statuses.add(result.get());
		}
		pool.shutdown();

		assertThat(statuses).containsOnly(201, 409).containsOnlyOnce(201);
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM project_member WHERE project_id = ? AND user_id = ?",
				Integer.class, this.project, this.cleo.id()))
			.isEqualTo(1);
	}

	@Test
	void letsOnlyTheProjectManagerManageMembers() throws Exception {
		addMember(this.ben, this.cleo.email())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_PROJECT_MANAGER"));
		this.mvc.perform(delete(members() + "/" + this.ana.id()).with(this.ben.signedIn()).with(csrf()))
			.andExpect(status().isForbidden());
		addMember(this.cleo, this.cleo.email())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
		this.mvc.perform(get(members()).with(this.cleo.signedIn())).andExpect(status().isNotFound());
	}

	@Test
	void removingAMemberRemovesTheirAccessAndTheirAssignmentsInThatProjectOnly() throws Exception {
		long task = this.data.task(this.project, "Shared task", "TODO", this.ana, this.ben);
		long otherProject = this.data.project("Other project", this.ana, this.ben);
		long otherTask = this.data.task(otherProject, "Other task", "TODO", this.ben);

		this.mvc.perform(delete(members() + "/" + this.ben.id()).with(this.ana.signedIn()).with(csrf()))
			.andExpect(status().isNoContent());

		this.mvc.perform(get("/projects/" + this.project).with(this.ben.signedIn())).andExpect(status().isNotFound());
		assertThat(assigneesOf(task)).isEqualTo(1);
		assertThat(assigneesOf(otherTask)).isEqualTo(1);
	}

	@Test
	void theProjectManagerCannotBeRemoved() throws Exception {
		this.mvc.perform(delete(members() + "/" + this.ana.id()).with(this.ana.signedIn()).with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PROJECT_MANAGER_NOT_REMOVABLE"));
	}

	@Test
	void removingSomeoneWhoIsNotAMemberIsNotFound() throws Exception {
		this.mvc.perform(delete(members() + "/" + this.cleo.id()).with(this.ana.signedIn()).with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	@Test
	void rejectsAnInvalidEmail() throws Exception {
		addMember(this.ana, "not-an-email")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
	}

	private String members() {
		return "/projects/" + this.project + "/members";
	}

	private ResultActions addMember(TestUser caller, String email) throws Exception {
		return this.mvc.perform(post(members()).with(caller.signedIn())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\"}"));
	}

	private int assigneesOf(long task) {
		return this.jdbc.queryForObject("SELECT COUNT(*) FROM task_assignee WHERE task_id = ?", Integer.class, task);
	}

}
