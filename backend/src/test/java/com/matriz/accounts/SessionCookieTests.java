package com.matriz.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

import com.matriz.TestcontainersConfiguration;

/**
 * Cookie settings on a real server (ADR 0004). MockMvc doesn't use the server's cookie
 * settings, so these are checked over HTTP.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SessionCookieTests {

	@Value("${local.server.port}")
	int port;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@AfterEach
	void removeTestUsers() {
		this.jdbc.update("DELETE FROM app_user WHERE email LIKE 'test-%'");
	}

	@Test
	void signInFlowUsesSecureCookieSettings() {
		RestClient client = RestClient.builder()
			.baseUrl("http://localhost:" + this.port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();
		String email = "test-" + UUID.randomUUID() + "@example.com";
		String password = UUID.randomUUID().toString();
		this.jdbc.update("INSERT INTO app_user (name, email, password_hash) VALUES ('Dana Test', ?, ?)", email,
				this.passwordEncoder.encode(password));

		// Before sign-in: 401, and the CSRF cookie the client needs for its first POST.
		ResponseEntity<String> me = client.get().uri("/users/me").retrieve().toEntity(String.class);
		assertThat(me.getStatusCode().value()).isEqualTo(401);
		String xsrfCookie = cookie(me, "XSRF-TOKEN");
		// Readable by the client's scripts on every page, not only under /api.
		assertThat(xsrfCookie).doesNotContain("HttpOnly").containsPattern("Path=/(;|$)");
		// No session is created before sign-in.
		assertThat(me.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE)).noneMatch(c -> c.startsWith("SESSION="));
		String xsrfToken = xsrfCookie.substring("XSRF-TOKEN=".length(), xsrfCookie.indexOf(';'));

		ResponseEntity<String> signIn = client.post()
			.uri("/sessions")
			.header(HttpHeaders.COOKIE, "XSRF-TOKEN=" + xsrfToken)
			.header("X-XSRF-TOKEN", xsrfToken)
			.contentType(MediaType.APPLICATION_JSON)
			.body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
			.retrieve()
			.toEntity(String.class);

		assertThat(signIn.getStatusCode().value()).isEqualTo(200);
		assertThat(cookie(signIn, "SESSION")).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/api");
	}

	private static String cookie(ResponseEntity<?> response, String name) {
		List<String> cookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
		return cookies.stream()
			.filter(cookie -> cookie.startsWith(name + "="))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No " + name + " cookie in " + cookies));
	}

}
