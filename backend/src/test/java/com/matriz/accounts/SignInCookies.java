package com.matriz.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

/**
 * Signs in a new test user over HTTP and returns the cookies the server sets. MockMvc
 * doesn't use the server's cookie settings, so cookie tests need a real server. Test users'
 * emails start with {@code test-}.
 */
final class SignInCookies {

	/** Set-Cookie values: XSRF-TOKEN before sign-in, SESSION after it. */
	record Cookies(String xsrfToken, String session) {
	}

	private SignInCookies() {
	}

	static Cookies signIn(int port, JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
		RestClient client = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();
		String email = "test-" + UUID.randomUUID() + "@example.com";
		String password = UUID.randomUUID().toString();
		jdbc.update("INSERT INTO app_user (name, email, password_hash) VALUES ('Dana Test', ?, ?)", email,
				passwordEncoder.encode(password));

		// Before sign-in: 401, and the CSRF cookie the client needs for its first POST.
		ResponseEntity<String> me = client.get().uri("/users/me").retrieve().toEntity(String.class);
		assertThat(me.getStatusCode().value()).isEqualTo(401);
		String xsrfCookie = cookie(me, "XSRF-TOKEN");
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
		return new Cookies(xsrfCookie, cookie(signIn, "SESSION"));
	}

	private static String cookie(ResponseEntity<?> response, String name) {
		List<String> cookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
		return cookies.stream()
			.filter(cookie -> cookie.startsWith(name + "="))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No " + name + " cookie in " + cookies));
	}

}
