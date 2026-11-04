package com.matriz.common.web;

/**
 * Error codes returned in the {@code code} field of every error response (docs/openapi.yaml).
 */
public enum ErrorCode {
	MALFORMED_REQUEST,
	VALIDATION_FAILED,
	UNAUTHENTICATED,
	INVALID_CREDENTIALS,
	CSRF_TOKEN_INVALID,
	TOO_MANY_SIGN_IN_ATTEMPTS,
	INTERNAL_ERROR
}
