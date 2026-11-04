package com.matriz.common.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * An expected failure that is returned to the client as a problem response with a code.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final ErrorCode code;

	public ApiException(HttpStatus status, ErrorCode code, String detail) {
		super(detail);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return this.status;
	}

	public ErrorCode getCode() {
		return this.code;
	}

	/**
	 * Extra headers for the response, such as {@code Retry-After}.
	 */
	public HttpHeaders getHeaders() {
		return HttpHeaders.EMPTY;
	}

}
