package com.matriz.common.security;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.matriz.common.web.ErrorCode;

/**
 * Access rules for the API (NFR-02, ADR 0004).
 *
 * <ul>
 * <li>The session is kept on the server (Spring Session JDBC) and identified by the
 * {@code SESSION} cookie.</li>
 * <li>Every POST, PUT and DELETE must send the {@code XSRF-TOKEN} cookie value in the
 * {@code X-XSRF-TOKEN} header.</li>
 * <li>Only sign-in, sign-out and the health check work without a session.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain apiSecurity(HttpSecurity http, ProblemResponseWriter problems) throws Exception {
		PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();
		// The client reads XSRF-TOKEN from pages outside /api, so the cookie must use path "/".
		CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokens.setCookiePath("/");
		return http
			.authorizeHttpRequests(requests -> requests
				.requestMatchers(path.matcher(HttpMethod.POST, "/sessions")).permitAll()
				.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
				.requestMatchers("/error").permitAll()
				.anyRequest().authenticated())
			.csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokens))
			.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			// An API has no page to return to after sign-in, so rejected requests aren't saved
			// (that would create a session for every anonymous request).
			.requestCache(cache -> cache.disable())
			.securityContext(context -> context.securityContextRepository(securityContextRepository()))
			.logout(logout -> logout
				.logoutRequestMatcher(path.matcher(HttpMethod.DELETE, "/sessions/current"))
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
				.deleteCookies("SESSION"))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint((request, response, ex) -> problems.write(request, response,
						HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "Sign in to continue."))
				.accessDeniedHandler((request, response, ex) -> {
					if (ex instanceof CsrfException) {
						problems.write(request, response, HttpStatus.FORBIDDEN, ErrorCode.CSRF_TOKEN_INVALID,
								"The request is missing a valid CSRF token.");
					}
					else {
						response.sendError(HttpStatus.FORBIDDEN.value());
					}
				}))
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.build();
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
