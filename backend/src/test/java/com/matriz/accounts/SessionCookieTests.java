package com.matriz.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.matriz.TestcontainersConfiguration;

/**
 * Cookie settings on a real server (ADR 0004), with the local default: no HTTPS, so the
 * cookies aren't {@code Secure}. {@link SecureCookieTests} checks the setting used on Render.
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
		SignInCookies.Cookies cookies = SignInCookies.signIn(this.port, this.jdbc, this.passwordEncoder);

		// Readable by the client's scripts on every page, not only under /api.
		assertThat(cookies.xsrfToken()).doesNotContain("HttpOnly").containsPattern("Path=/(;|$)");
		assertThat(cookies.session()).contains("HttpOnly").contains("SameSite=Lax").contains("Path=/api");
		// Locally there is no HTTPS, so a Secure cookie would never be sent back.
		assertThat(cookies.xsrfToken()).doesNotContain("Secure");
		assertThat(cookies.session()).doesNotContain("Secure");
	}

}
