package com.matriz.tasks;

import org.springframework.http.HttpStatus;

import com.matriz.common.web.ApiException;
import com.matriz.common.web.ErrorCode;

/**
 * Errors of the Tasks module, as described in docs/openapi.yaml.
 */
final class TaskErrors {

	private TaskErrors() {
	}

	/**
	 * The project doesn't exist, or the caller isn't a member: both look the same (EC-04).
	 */
	static ApiException projectNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, "Project not found.");
	}

	static ApiException taskNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.TASK_NOT_FOUND, "Task not found.");
	}

	static ApiException notProjectManager() {
		return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.NOT_PROJECT_MANAGER,
				"Only the project manager can do this.");
	}

}
