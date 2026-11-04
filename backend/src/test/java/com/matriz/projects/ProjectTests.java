package com.matriz.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import tools.jackson.databind.json.JsonMapper;

import com.matriz.TestData;
import com.matriz.TestData.TestUser;
import com.matriz.TestcontainersConfiguration;

/**
 * Projects: create, list, search, open, edit and delete, with access rules (US-02, US-12,
 * EC-03, EC-04, Practical Sheet 03 Exercise 4).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProjectTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	JsonMapper json;

	TestData data;

	TestUser ana;

	TestUser ben;

	@BeforeEach
	void createUsers() {
		this.data = new TestData(this.jdbc);
		this.ana = this.data.user("Ana Test");
		this.ben = this.data.user("Ben Test");
	}

	@AfterEach
	void removeTestData() {
		this.data.deleteAll();
	}

	@Test
	void createsAProjectThatIsSavedAndListed() throws Exception {
		String location = createProject(this.ana, "Garden plan", "Spring planting.")
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", endsWith("/projects/" + idOfProject("Garden plan"))))
			.andExpect(jsonPath("$.name").value("Garden plan"))
			.andExpect(jsonPath("$.description").value("Spring planting."))
			.andExpect(jsonPath("$.myRole").value("PROJECT_MANAGER"))
			.andExpect(jsonPath("$.taskCounts.todo").value(0))
			.andExpect(jsonPath("$.createdAt").isString())
			.andReturn()
			.getResponse()
			.getHeader("Location");

		// Read back from the database, then through the API.
		assertThat(this.jdbc.queryForObject("SELECT role FROM project_member WHERE project_id = ? AND user_id = ?",
				String.class, idOfProject("Garden plan"), this.ana.id()))
			.isEqualTo("PROJECT_MANAGER");
		this.mvc.perform(get(location.substring(location.indexOf("/projects"))).with(this.ana.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Garden plan"));
		this.mvc.perform(get("/projects").with(this.ana.signedIn()))
			.andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("Garden plan")));
	}

	@Test
	void trimsTheNameAndStoresAnEmptyDescriptionAsNone() throws Exception {
		createProject(this.ana, "   Garden plan  ", "   ")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Garden plan"))
			.andExpect(jsonPath("$.description").doesNotExist());
	}

	@Test
	void listsOnlyTheCallersProjectsSortedByName() throws Exception {
		this.data.project("Zebra study", this.ana);
		this.data.project("apple orchard", this.ana, this.ben);
		this.data.project("Ben's own", this.ben);

		this.mvc.perform(get("/projects").with(this.ana.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("apple orchard", "Zebra study")))
			.andExpect(jsonPath("$[0].myRole").value("PROJECT_MANAGER"));
		this.mvc.perform(get("/projects").with(this.ben.signedIn()))
			.andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("apple orchard", "Ben's own")))
			.andExpect(jsonPath("$[0].myRole").value("MEMBER"));
	}

	@Test
	void searchesTheCallersProjectsByNameIgnoringCase() throws Exception {
		this.data.project("Website redesign", this.ana);
		this.data.project("Mobile launch", this.ana);
		this.data.project("Web shop", this.ben);

		this.mvc.perform(get("/projects").param("q", " WEB ").with(this.ana.signedIn()))
			.andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("Website redesign")));
		this.mvc.perform(get("/projects").param("q", "nothing like this").with(this.ana.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
		this.mvc.perform(get("/projects").param("q", "").with(this.ana.signedIn()))
			.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void searchTreatsPercentAndUnderscoreAsPlainCharacters() throws Exception {
		this.data.project("100% done", this.ana);
		this.data.project("Other project", this.ana);

		this.mvc.perform(get("/projects").param("q", "%").with(this.ana.signedIn()))
			.andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("100% done")));
		this.mvc.perform(get("/projects").param("q", "_").with(this.ana.signedIn()))
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void hidesProjectsFromNonMembersEvenWhenTheIdIsChanged() throws Exception {
		long bensProject = this.data.project("Ben's own", this.ben);

		for (ResultActions result : new ResultActions[] {
				this.mvc.perform(get("/projects/" + bensProject).with(this.ana.signedIn())),
				this.mvc.perform(put("/projects/" + bensProject).with(this.ana.signedIn()).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(body("Taken over", null))),
				this.mvc.perform(delete("/projects/" + bensProject).with(this.ana.signedIn()).with(csrf())),
				this.mvc.perform(get("/projects/999999999").with(this.ana.signedIn())) }) {
			result.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
		}
		assertThat(this.jdbc.queryForObject("SELECT name FROM project WHERE id = ?", String.class, bensProject))
			.isEqualTo("Ben's own");
	}

	@Test
	void letsOnlyTheProjectManagerEditAndDelete() throws Exception {
		long project = this.data.project("Shared", this.ana, this.ben);

		this.mvc.perform(get("/projects/" + project).with(this.ben.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.myRole").value("MEMBER"));
		this.mvc.perform(put("/projects/" + project).with(this.ben.signedIn()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body("Renamed", null)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_PROJECT_MANAGER"));
		this.mvc.perform(delete("/projects/" + project).with(this.ben.signedIn()).with(csrf()))
			.andExpect(status().isForbidden());

		this.mvc.perform(put("/projects/" + project).with(this.ana.signedIn()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body("Renamed", "Now with a description")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Renamed"))
			.andExpect(jsonPath("$.description").value("Now with a description"));
		this.mvc.perform(get("/projects/" + project).with(this.ben.signedIn()))
			.andExpect(jsonPath("$.name").value("Renamed"));
	}

	@Test
	void deletingAProjectDeletesItsTasksMembershipsAndAssignments() throws Exception {
		long project = this.data.project("To delete", this.ana, this.ben);
		long task = this.data.task(project, "A task", "TODO", this.ben);
		this.jdbc.update("INSERT INTO comment (task_id, author_id, content, created_at) VALUES (?, ?, 'Hi', NOW())",
				task, this.ben.id());

		this.mvc.perform(delete("/projects/" + project).with(this.ana.signedIn()).with(csrf()))
			.andExpect(status().isNoContent());

		for (String sql : new String[] { "SELECT COUNT(*) FROM project WHERE id = ?",
				"SELECT COUNT(*) FROM project_member WHERE project_id = ?",
				"SELECT COUNT(*) FROM task WHERE project_id = ?" }) {
			assertThat(this.jdbc.queryForObject(sql, Integer.class, project)).as(sql).isZero();
		}
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM task_assignee WHERE task_id = ?", Integer.class,
				task))
			.isZero();
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM comment WHERE task_id = ?", Integer.class, task))
			.isZero();
		this.mvc.perform(get("/projects").with(this.ben.signedIn())).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void countsTasksPerStatusOverAllOfTheProjectsTasks() throws Exception {
		long project = this.data.project("Counted", this.ana);
		this.data.task(project, "One", "TODO");
		this.data.task(project, "Two", "TODO");
		this.data.task(project, "Three", "IN_PROGRESS");
		this.data.task(project, "Four", "DONE");

		this.mvc.perform(get("/projects").with(this.ana.signedIn()))
			.andExpect(jsonPath("$[0].taskCounts.todo").value(2))
			.andExpect(jsonPath("$[0].taskCounts.inProgress").value(1))
			.andExpect(jsonPath("$[0].taskCounts.done").value(1));
		this.mvc.perform(get("/projects/" + project).with(this.ana.signedIn()))
			.andExpect(jsonPath("$.taskCounts.todo").value(2));
	}

	@Test
	void rejectsInvalidNamesAndDescriptions() throws Exception {
		expectFieldError(createProject(this.ana, "   ", null), "name", "BLANK");
		expectFieldError(createProject(this.ana, "  ab  ", null), "name", "TOO_SHORT");
		expectFieldError(createProject(this.ana, "x".repeat(101), null), "name", "TOO_LONG");
		expectFieldError(createProject(this.ana, "Good name", "x".repeat(1001)), "description", "TOO_LONG");
		expectFieldError(this.mvc.perform(post("/projects").with(this.ana.signedIn()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}")), "name", "REQUIRED");

		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM project_member WHERE user_id = ?", Integer.class,
				this.ana.id()))
			.isZero();
	}

	@Test
	void rejectsAnInvalidProjectId() throws Exception {
		this.mvc.perform(get("/projects/abc").with(this.ana.signedIn()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
		this.mvc.perform(get("/projects/0").with(this.ana.signedIn()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void requiresSignIn() throws Exception {
		this.mvc.perform(get("/projects")).andExpect(status().isUnauthorized());
	}

	private ResultActions createProject(TestUser user, String name, String description) throws Exception {
		return this.mvc.perform(post("/projects").with(user.signedIn())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(name, description)));
	}

	private String body(String name, String description) {
		java.util.Map<String, Object> body = new java.util.HashMap<>();
		body.put("name", name);
		body.put("description", description);
		return this.json.writeValueAsString(body);
	}

	private long idOfProject(String name) {
		return this.jdbc.queryForObject("SELECT id FROM project WHERE name = ? ORDER BY id DESC LIMIT 1", Long.class,
				name);
	}

	private static void expectFieldError(ResultActions result, String field, String code) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors", hasSize(1)))
			.andExpect(jsonPath("$.errors[0].field").value(field))
			.andExpect(jsonPath("$.errors[0].code").value(code));
	}

}
