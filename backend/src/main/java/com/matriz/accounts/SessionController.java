package com.matriz.accounts;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-in (US-01). Sign-out ({@code DELETE /sessions/current}) is handled by the security
 * configuration.
 */
@RestController
class SessionController {

	private final SignInService signInService;

	private final SecurityContextRepository securityContextRepository;

	private final SecurityContextHolderStrategy securityContextHolder = SecurityContextHolder
		.getContextHolderStrategy();

	SessionController(SignInService signInService, SecurityContextRepository securityContextRepository) {
		this.signInService = signInService;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/sessions")
	UserResponse signIn(@Valid @RequestBody SignInRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		User user = this.signInService.signIn(body.email(), body.password());
		startSession(new AuthenticatedUser(user.getId(), user.getEmail()), request, response);
		return UserResponse.of(user);
	}

	/**
	 * Stores the user in the session, with a new session ID to prevent session fixation.
	 */
	private void startSession(AuthenticatedUser user, HttpServletRequest request, HttpServletResponse response) {
		HttpSession existing = request.getSession(false);
		if (existing != null) {
			request.changeSessionId();
		}
		SecurityContext context = this.securityContextHolder.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
		this.securityContextHolder.setContext(context);
		this.securityContextRepository.saveContext(context, request, response);
	}

}
