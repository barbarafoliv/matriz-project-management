package com.matriz.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
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

import com.matriz.TestcontainersConfiguration;

/**
 * Sign-in, sign-out and the signed-in user (US-01, EC-02, NFR-02, Practical Sheet 03 Exercise 3).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SignInTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@AfterEach
	void removeTestUsers() {
		this.jdbc.update("DELETE FROM sign_in_attempt WHERE email LIKE 'test-%'");
		this.jdbc.update("DELETE FROM app_user WHERE email LIKE 'test-%'");
	}

	@Test
	void signsInWithCorrectCredentialsAndStartsASession() throws Exception {
		TestUser user = createUser("Dana Test");

		Cookie session = signIn(user.email(), user.password())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Dana Test"))
			.andExpect(jsonPath("$.email").value(user.email()))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(cookie().exists("SESSION"))
			.andReturn()
			.getResponse()
			.getCookie("SESSION");

		this.mvc.perform(get("/users/me").cookie(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(user.email()));
	}

	@Test
	void ignoresUpperAndLowerCaseInTheEmail() throws Exception {
		TestUser user = createUser("Dana Test");

		signIn(user.email().toUpperCase(), user.password()).andExpect(status().isOk());
	}

	@Test
	void rejectsAWrongPasswordAndAnUnknownEmailWithTheSameMessage() throws Exception {
		TestUser user = createUser("Dana Test");

		expectInvalidCredentials(signIn(user.email(), "wrong-password"));
		expectInvalidCredentials(signIn("test-nobody@example.com", user.password()));
	}

	@Test
	void requiresASessionForTheSignedInUser() throws Exception {
		this.mvc.perform(get("/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void rejectsASignInWithoutTheCsrfToken() throws Exception {
		TestUser user = createUser("Dana Test");

		this.mvc.perform(post("/sessions").contentType(MediaType.APPLICATION_JSON).content(json(user.email(), user.password())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
	}

	@Test
	void reportsInvalidFields() throws Exception {
		signIn("not-an-email", "")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[?(@.field == 'email')].code").value("INVALID_FORMAT"))
			.andExpect(jsonPath("$.errors[?(@.field == 'password')].code").value("REQUIRED"));

		signIn("   ", "x")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'email')].code").value("BLANK"));
	}

	@Test
	void countsTheLengthOfTheEmailAndPasswordInCharacters() throws Exception {
		String emoji = "😀"; // one character, two UTF-16 units

		// 64 characters is allowed (docs/constraints.md): the password is checked, and it's wrong,
		// even though it's 256 bytes and BCrypt only uses the first 72.
		expectInvalidCredentials(signIn("test-nobody@example.com", emoji.repeat(64)));
		expectInvalidCredentials(signIn(createUser("Eve Test").email(), emoji.repeat(64)));

		signIn("test-nobody@example.com", emoji.repeat(65))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'password')].code").value("TOO_LONG"));
		signIn("test-nobody@example.com", "x".repeat(65))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'password')].code").value("TOO_LONG"));
		signIn("x".repeat(243) + "@example.com", "x")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'email')].code").value("TOO_LONG"));
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		this.mvc.perform(post("/sessions").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void blocksSignInAfterFiveFailedAttemptsEvenWithTheRightPassword() throws Exception {
		TestUser user = createUser("Dana Test");
		for (int i = 0; i < SignInService.MAX_FAILED_ATTEMPTS; i++) {
			expectInvalidCredentials(signIn(user.email(), "wrong-password"));
		}

		signIn(user.email(), user.password())
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(jsonPath("$.code").value("TOO_MANY_SIGN_IN_ATTEMPTS"))
			.andExpect(jsonPath("$.detail").value("Too many failed sign-in attempts. Try again in 15 minutes."));

		long retryAfter = Long.parseLong(signIn(user.email(), user.password()).andReturn()
			.getResponse()
			.getHeader("Retry-After"));
		assertThat(retryAfter).isBetween(1L, SignInService.LOCK_DURATION.toSeconds());
	}

	@Test
	void allowsSignInAgainWhenTheBlockHasExpired() throws Exception {
		TestUser user = createUser("Dana Test");
		for (int i = 0; i < SignInService.MAX_FAILED_ATTEMPTS; i++) {
			signIn(user.email(), "wrong-password");
		}
		this.jdbc.update("UPDATE sign_in_attempt SET locked_until = NOW() - INTERVAL '1 second' WHERE email = ?",
				user.email());

		signIn(user.email(), user.password()).andExpect(status().isOk());
		assertThat(attemptRows(user.email())).isZero();
	}

	@Test
	void clearsTheFailedCountAfterASuccessfulSignIn() throws Exception {
		TestUser user = createUser("Dana Test");
		for (int i = 0; i < SignInService.MAX_FAILED_ATTEMPTS - 1; i++) {
			signIn(user.email(), "wrong-password");
		}

		signIn(user.email(), user.password()).andExpect(status().isOk());
		assertThat(attemptRows(user.email())).isZero();
	}

	@Test
	void doesNotCountFailuresForEmailsWithoutAnAccount() throws Exception {
		signIn("test-nobody@example.com", "wrong-password");

		assertThat(attemptRows("test-nobody@example.com")).isZero();
	}

	@Test
	void signsOutAndEndsTheSession() throws Exception {
		TestUser user = createUser("Dana Test");
		Cookie session = signIn(user.email(), user.password()).andReturn().getResponse().getCookie("SESSION");

		this.mvc.perform(delete("/sessions/current").with(csrf()).cookie(session))
			.andExpect(status().isNoContent())
			.andExpect(cookie().maxAge("SESSION", 0));

		this.mvc.perform(get("/users/me").cookie(session)).andExpect(status().isUnauthorized());
	}

	@Test
	void signOutSucceedsWithoutASession() throws Exception {
		this.mvc.perform(delete("/sessions/current").with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	void eachSessionBelongsToItsOwnUser() throws Exception {
		TestUser dana = createUser("Dana Test");
		TestUser eli = createUser("Eli Test");
		Cookie danaSession = signIn(dana.email(), dana.password()).andReturn().getResponse().getCookie("SESSION");
		Cookie eliSession = signIn(eli.email(), eli.password()).andReturn().getResponse().getCookie("SESSION");

		this.mvc.perform(get("/users/me").cookie(danaSession)).andExpect(jsonPath("$.email").value(dana.email()));
		this.mvc.perform(get("/users/me").cookie(eliSession)).andExpect(jsonPath("$.email").value(eli.email()));
	}

	@Test
	void sessionsExpireAfterThirtyMinutesOfInactivity() throws Exception {
		TestUser user = createUser("Dana Test");
		signIn(user.email(), user.password()).andExpect(status().isOk());

		Integer maxInactive = this.jdbc.queryForObject(
				"SELECT max_inactive_interval FROM spring_session WHERE principal_name = ?", Integer.class,
				user.email());
		assertThat(maxInactive).isEqualTo(30 * 60);
	}

	@Test
	void demoAccountsAcceptThePasswordListedInTheReadme() throws Exception {
		signIn("alice@example.com", demoPasswordFromReadme()).andExpect(status().isOk());
	}

	private ResultActions signIn(String email, String password) throws Exception {
		return this.mvc.perform(
				post("/sessions").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(email, password)));
	}

	private static void expectInvalidCredentials(ResultActions result) throws Exception {
		result.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.detail").value("Email or password is incorrect."));
	}

	private TestUser createUser(String name) {
		String email = "test-" + UUID.randomUUID() + "@example.com";
		String password = UUID.randomUUID().toString();
		this.jdbc.update("INSERT INTO app_user (name, email, password_hash) VALUES (?, ?, ?)", name, email,
				this.passwordEncoder.encode(password));
		return new TestUser(email, password);
	}

	private int attemptRows(String email) {
		return this.jdbc.queryForObject("SELECT COUNT(*) FROM sign_in_attempt WHERE email = ?", Integer.class,
				email.toLowerCase());
	}

	private static String json(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	/**
	 * The demo password is only written in the README (AGENTS.md), so it is read from there.
	 */
	private static String demoPasswordFromReadme() throws IOException {
		String readme = Files.readString(Path.of("..", "README.md"));
		Matcher matcher = Pattern.compile("fake password \\*\\*`([^`]+)`\\*\\*").matcher(readme);
		assertThat(matcher.find()).as("demo password in README.md").isTrue();
		return matcher.group(1);
	}

	private record TestUser(String email, String password) {
	}

}
