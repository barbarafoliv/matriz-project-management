package com.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import com.matriz.TestData;
import com.matriz.TestData.TestUser;
import com.matriz.TestcontainersConfiguration;

/**
 * Comments: add and list, with the author and date set by the server (US-11, EC-10,
 * Practical Sheet 03 Exercises 3 and 6).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CommentTests {

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

	long task;

	@BeforeEach
	void createTask() {
		this.data = new TestData(this.jdbc);
		this.ana = this.data.user("Ana Test");
		this.ben = this.data.user("Ben Test");
		this.cleo = this.data.user("Cleo Test");
		this.project = this.data.project("Team project", this.ana, this.ben);
		this.task = this.data.task(this.project, "Discussed task", "TODO");
	}

	@AfterEach
	void removeTestData() {
		this.data.deleteAll();
	}

	@Test
	void addsACommentWithTheSignedInUserAsAuthorAndTheServersDate() throws Exception {
		Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		addComment(this.ben, "  First draft is ready.  ").andExpect(status().isCreated())
			.andExpect(jsonPath("$.content").value("First draft is ready."))
			.andExpect(jsonPath("$.taskId").value(this.task))
			.andExpect(jsonPath("$.author.id").value(this.ben.id()))
			.andExpect(jsonPath("$.author.name").value("Ben Test"))
			.andExpect(jsonPath("$.createdAt").isString());

		Map<String, Object> saved = this.jdbc.queryForMap("SELECT author_id, content, created_at FROM comment WHERE task_id = ?",
				this.task);
		assertThat(saved.get("author_id")).isEqualTo(this.ben.id());
		assertThat(saved.get("content")).isEqualTo("First draft is ready.");
		assertThat(((Timestamp) saved.get("created_at")).toInstant()).isAfterOrEqualTo(before);
	}

	@Test
	void ignoresAnAuthorOrDateSentInTheRequest() throws Exception {
		String body = this.json.writeValueAsString(Map.of("content", "Who wrote this?", "author",
				Map.of("id", this.ana.id(), "name", "Ana Test"), "authorId", this.ana.id(), "createdAt",
				"2000-01-01T00:00:00Z"));

		this.mvc.perform(post(comments()).with(this.ben.signedIn()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.author.id").value(this.ben.id()));

		assertThat(this.jdbc.queryForObject("SELECT author_id FROM comment WHERE task_id = ?", Long.class, this.task))
			.isEqualTo(this.ben.id());
		assertThat(this.jdbc.queryForObject("SELECT EXTRACT(YEAR FROM created_at)::int FROM comment WHERE task_id = ?",
				Integer.class, this.task))
			.isGreaterThan(2000);
	}

	@Test
	void listsCommentsOldestFirstWithAuthorAndDate() throws Exception {
		addComment(this.ana, "One");
		addComment(this.ben, "Two");
		addComment(this.ana, "Three");

		// Still there when the task is opened again, by any member.
		this.mvc.perform(get(comments()).with(this.ben.signedIn()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].content").value(contains("One", "Two", "Three")))
			.andExpect(jsonPath("$[*].author.name").value(contains("Ana Test", "Ben Test", "Ana Test")))
			.andExpect(jsonPath("$[0].createdAt").isString());
	}

	@Test
	void rejectsEmptyBlankAndTooLongComments() throws Exception {
		expectFieldError(addComment(this.ana, ""), "REQUIRED");
		expectFieldError(addComment(this.ana, "    "), "BLANK");
		expectFieldError(addComment(this.ana, "x".repeat(2001)), "TOO_LONG");
		addComment(this.ana, "x".repeat(2000)).andExpect(status().isCreated());

		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM comment WHERE task_id = ?", Integer.class, this.task))
			.isEqualTo(1);
	}

	@Test
	void showsCommentsOfDeletedAccountsWithoutAnAuthor() throws Exception {
		addComment(this.ben, "Written by Ben");
		this.jdbc.update("DELETE FROM project_member WHERE user_id = ?", this.ben.id());
		this.jdbc.update("DELETE FROM app_user WHERE id = ?", this.ben.id());

		this.mvc.perform(get(comments()).with(this.ana.signedIn()))
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].content").value("Written by Ben"))
			.andExpect(jsonPath("$[0].author").doesNotExist());
	}

	@Test
	void nonMembersCannotReadOrAddCommentsEvenWhenTheIdsAreChanged() throws Exception {
		addComment(this.ana, "Private");

		this.mvc.perform(get(comments()).with(this.cleo.signedIn()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
		addComment(this.cleo, "Sneaky").andExpect(status().isNotFound());

		long cleosProject = this.data.project("Cleo's project", this.cleo);
		this.mvc.perform(get("/projects/" + cleosProject + "/tasks/" + this.task + "/comments").with(this.cleo.signedIn()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
		assertThat(this.jdbc.queryForObject("SELECT COUNT(*) FROM comment WHERE task_id = ?", Integer.class, this.task))
			.isEqualTo(1);
	}

	private String comments() {
		return "/projects/" + this.project + "/tasks/" + this.task + "/comments";
	}

	private ResultActions addComment(TestUser author, String content) throws Exception {
		return this.mvc.perform(post(comments()).with(author.signedIn())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(this.json.writeValueAsString(Map.of("content", content))));
	}

	private static void expectFieldError(ResultActions result, String code) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors", hasSize(1)))
			.andExpect(jsonPath("$.errors[0].field").value("content"))
			.andExpect(jsonPath("$.errors[0].code").value(code));
	}

}
