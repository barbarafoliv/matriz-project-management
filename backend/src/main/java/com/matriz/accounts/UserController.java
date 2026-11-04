package com.matriz.accounts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.common.web.ApiException;
import com.matriz.common.web.ErrorCode;

/**
 * The signed-in user (US-01) and deleting their account (US-09).
 */
@RestController
class UserController {

	private final AccountService accountService;

	private final AccountDeletionService accountDeletionService;

	UserController(AccountService accountService, AccountDeletionService accountDeletionService) {
		this.accountService = accountService;
		this.accountDeletionService = accountDeletionService;
	}

	@GetMapping("/users/me")
	UserResponse currentUser(@AuthenticationPrincipal AuthenticatedUser principal) {
		return this.accountService.findUser(principal.id())
			.map(UserResponse::of)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED,
					"Sign in to continue."));
	}

	/**
	 * Deletes the account and signs the user out; the response expires the session cookie.
	 */
	@DeleteMapping("/users/me")
	ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AuthenticatedUser principal,
			HttpServletRequest request) {
		this.accountDeletionService.deleteAccount(principal.id());
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		return ResponseEntity.noContent().build();
	}

}
