package com.matriz.common.web;

import java.net.URI;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * Builds RFC 9457 problem details with the {@code code} field required by the API contract.
 */
public final class Problems {

	private Problems() {
	}

	public static ProblemDetail of(HttpStatusCode status, ErrorCode code, String detail, String instance) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setProperty("code", code);
		if (instance != null) {
			problem.setInstance(URI.create(instance));
		}
		return problem;
	}

}
