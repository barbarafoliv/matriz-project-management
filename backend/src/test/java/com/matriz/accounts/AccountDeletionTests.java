package com.matriz.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.matriz.TestData;
import com.matriz.TestData.TestUser;
import com.matriz.TestcontainersConfiguration;

/**
 * Deleting an account and its personal data (FR-14, US-09, EC-06).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AccountDeletionTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	AccountDeletionService accountDeletion;

	@Autowired
	PlatformTransactionManager transactionManager;

	TestData data;

	TestUser ana;

	TestUser ben;

	String password;

	long project;

	long task;

	@BeforeEach
	void createUsers() {
		this.data = new TestData(this.jdbc);
		this.password = UUID.randomUUID().toString();
		this.ana = this.data.user("Ana Test");
		this.ben = this.data.user("Ben Test");
		this.jdbc.update("UPDATE app_user SET password_hash = ? WHERE id = ?", this.passwordEncoder.encode(this.password),
				this.ben.id());
		this.project = this.data.project("Team project", this.ana, this.ben);
		this.task = this.data.task(this.project, "Shared task", "TODO", this.ana, this.ben);
		this.jdbc.update("INSERT INTO comment (task_id, author_id, content, created_at) VALUES (?, ?, 'Ben was here', NOW())",
				this.task, this.ben.id());
	}

	@AfterEach
	void removeTestData() {
		this.data.deleteAll();
	}

	@Test
	void deletesTheAccountAndSignsTheUserOut() throws Exception {
		Cookie session = signIn(this.ben.email(), this.password);

		this.mvc.perform(delete("/users/me").cookie(session).with(csrf()))
			.andExpect(status().isNoContent())
			.andExpect(cookie().maxAge("SESSION", 0));

		this.mvc.perform(get("/users/me").cookie(session)).andExpect(status().isUnauthorized());
		assertThat(count("SELECT COUNT(*) FROM app_user WHERE id = ?", this.ben.id())).isZero();
		assertThat(count("SELECT COUNT(*) FROM spring_session WHERE principal_name = ?", this.ben.email())).isZero();
	}

	@Test
	void theDeletedAccountCanNoLongerSignInButTheEmailCanBeUsedAgain() throws Exception {
		Cookie session = signIn(this.ben.email(), this.password);
		this.mvc.perform(delete("/users/me").cookie(session).with(csrf())).andExpect(status().isNoContent());

		this.mvc.perform(signInRequest(this.ben.email(), this.password))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

		this.jdbc.update("INSERT INTO app_user (name, email, password_hash) VALUES ('New Ben', ?, 'x')",
				this.ben.email().toUpperCase());
		assertThat(count("SELECT COUNT(*) FROM app_user WHERE LOWER(email) = LOWER(?)", this.ben.email())).isEqualTo(1);
	}

	@Test
	void removesTheirMembershipsAndAssignmentsAndKeepsTheirCommentsWithoutAuthor() throws Exception {
		Cookie session = signIn(this.ben.email(), this.password);

		this.mvc.perform(delete("/users/me").cookie(session).with(csrf())).andExpect(status().isNoContent());

		// Other members no longer see their name or email (US-09).
		this.mvc.perform(get("/projects/" + this.project + "/members").with(this.ana.signedIn()))
			.andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("Ana Test")));
		this.mvc.perform(get("/projects/" + this.project + "/tasks/" + this.task).with(this.ana.signedIn()))
			.andExpect(jsonPath("$.assignees[*].name").value(org.hamcrest.Matchers.contains("Ana Test")));
		this.mvc.perform(get("/projects/" + this.project + "/tasks/" + this.task + "/comments").with(this.ana.signedIn()))
			.andExpect(jsonPath("$[0].content").value("Ben was here"))
			.andExpect(jsonPath("$[0].author").doesNotExist());
		assertThat(count("SELECT COUNT(*) FROM project_member WHERE user_id = ?", this.ben.id())).isZero();
		assertThat(count("SELECT COUNT(*) FROM task_assignee WHERE user_id = ?", this.ben.id())).isZero();
	}

	@Test
	void removesTheirFailedSignInRecord() throws Exception {
		this.mvc.perform(signInRequest(this.ben.email(), "wrong-password")).andExpect(status().isUnauthorized());
		assertThat(count("SELECT COUNT(*) FROM sign_in_attempt WHERE email = ?", this.ben.email())).isEqualTo(1);
		Cookie session = signIn(this.ben.email(), this.password);
		this.mvc.perform(signInRequest(this.ben.email(), "wrong-password"));

		this.mvc.perform(delete("/users/me").cookie(session).with(csrf())).andExpect(status().isNoContent());

		assertThat(count("SELECT COUNT(*) FROM sign_in_attempt WHERE email = ?", this.ben.email())).isZero();
	}

	@Test
	void endsTheUsersOtherSessionsToo() throws Exception {
		Cookie laptop = signIn(this.ben.email(), this.password);
		Cookie phone = signIn(this.ben.email(), this.password);

		this.mvc.perform(delete("/users/me").cookie(laptop).with(csrf())).andExpect(status().isNoContent());

		this.mvc.perform(get("/users/me").cookie(phone)).andExpect(status().isUnauthorized());
	}

	@Test
	void keepsTheSessionsWhenTheDeletionIsNotCommitted() throws Exception {
		Cookie session = signIn(this.ben.email(), this.password);

		// The deletion fails after the service has run, so its transaction is rolled back.
		new TransactionTemplate(this.transactionManager).executeWithoutResult((status) -> {
			this.accountDeletion.deleteAccount(this.ben.id());
			status.setRollbackOnly();
		});

		assertThat(count("SELECT COUNT(*) FROM app_user WHERE id = ?", this.ben.id())).isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM spring_session WHERE principal_name = ?", this.ben.email())).isEqualTo(1);
		this.mvc.perform(get("/users/me").cookie(session)).andExpect(status().isOk());
	}

	@Test
	void aProjectManagerCannotDeleteTheirAccount() throws Exception {
		this.jdbc.update("UPDATE app_user SET password_hash = ? WHERE id = ?", this.passwordEncoder.encode(this.password),
				this.ana.id());
		Cookie session = signIn(this.ana.email(), this.password);

		this.mvc.perform(delete("/users/me").cookie(session).with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ACCOUNT_MANAGES_PROJECTS"))
			.andExpect(jsonPath("$.detail").value("Delete the projects you manage before deleting your account."));

		assertThat(count("SELECT COUNT(*) FROM app_user WHERE id = ?", this.ana.id())).isEqualTo(1);
		this.mvc.perform(get("/users/me").cookie(session)).andExpect(status().isOk());
	}

	@Test
	void requiresASessionAndTheCsrfToken() throws Exception {
		this.mvc.perform(delete("/users/me").with(csrf())).andExpect(status().isUnauthorized());
		Cookie session = signIn(this.ben.email(), this.password);
		this.mvc.perform(delete("/users/me").cookie(session))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
		assertThat(count("SELECT COUNT(*) FROM app_user WHERE id = ?", this.ben.id())).isEqualTo(1);
	}

	private Cookie signIn(String email, String password) throws Exception {
		ResultActions result = this.mvc.perform(signInRequest(email, password)).andExpect(status().isOk());
		return result.andReturn().getResponse().getCookie("SESSION");
	}

	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder signInRequest(String email,
			String password) {
		return post("/sessions").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
	}

	private int count(String sql, Object argument) {
		return this.jdbc.queryForObject(sql, Integer.class, argument);
	}

}
