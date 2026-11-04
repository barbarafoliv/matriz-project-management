package com.matriz.accounts;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.matriz.common.web.ApiException;
import com.matriz.common.web.ErrorCode;

/**
 * Sign-in is blocked after too many failed attempts (NFR-02).
 */
class TooManySignInAttemptsException extends ApiException {

	private final long retryAfterSeconds;

	TooManySignInAttemptsException(Duration retryAfter) {
		super(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_SIGN_IN_ATTEMPTS,
				"Too many failed sign-in attempts. Try again in 15 minutes.");
		this.retryAfterSeconds = Math.max(1, retryAfter.toSeconds());
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.RETRY_AFTER, Long.toString(this.retryAfterSeconds));
		return headers;
	}

}
