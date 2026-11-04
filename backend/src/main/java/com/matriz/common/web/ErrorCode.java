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
	NOT_PROJECT_MANAGER,
	PROJECT_NOT_FOUND,
	MEMBER_NOT_FOUND,
	ALREADY_MEMBER,
	PROJECT_MANAGER_NOT_REMOVABLE,
	TOO_MANY_SIGN_IN_ATTEMPTS,
	INTERNAL_ERROR
}
