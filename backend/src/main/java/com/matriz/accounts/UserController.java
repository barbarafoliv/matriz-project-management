package com.matriz.accounts;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.common.web.ApiException;
import com.matriz.common.web.ErrorCode;

/**
 * The signed-in user (US-01).
 */
@RestController
class UserController {

	private final AccountService accountService;

	UserController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping("/users/me")
	UserResponse currentUser(@AuthenticationPrincipal AuthenticatedUser principal) {
		return this.accountService.findUser(principal.id())
			.map(UserResponse::of)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED,
					"Sign in to continue."));
	}

}
