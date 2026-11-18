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
 * With {@code COOKIES_SECURE=true}, as on Render (ADR 0008), both cookies are only sent
 * over HTTPS, and keep their other settings.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "COOKIES_SECURE=true")
class SecureCookieTests {

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
	void sessionAndCsrfCookiesAreSecure() {
		SignInCookies.Cookies cookies = SignInCookies.signIn(this.port, this.jdbc, this.passwordEncoder);

		assertThat(cookies.xsrfToken()).contains("Secure").doesNotContain("HttpOnly").containsPattern("Path=/(;|$)");
		assertThat(cookies.session()).contains("Secure").contains("HttpOnly").contains("SameSite=Lax")
			.contains("Path=/api");
	}

}
