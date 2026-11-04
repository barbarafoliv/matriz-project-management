package com.matriz.common.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import com.matriz.common.web.ErrorCode;
import com.matriz.common.web.Problems;

/**
 * Writes problem responses from the security filters, which run before the controllers.
 */
@Component
class ProblemResponseWriter {

	private final JsonMapper json;

	ProblemResponseWriter(JsonMapper json) {
		this.json = json;
	}

	void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, ErrorCode code,
			String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		this.json.writeValue(response.getOutputStream(), Problems.of(status, code, detail, request.getRequestURI()));
	}

}
