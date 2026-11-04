package com.matriz.projects;

import org.springframework.http.HttpStatus;

import com.matriz.common.web.ApiException;
import com.matriz.common.web.ErrorCode;

/**
 * Errors of the Projects module, as described in docs/openapi.yaml.
 */
final class ProjectErrors {

	private ProjectErrors() {
	}

	/**
	 * The project doesn't exist, or the caller isn't a member: both look the same (EC-04).
	 */
	static ApiException projectNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, "Project not found.");
	}

	static ApiException notProjectManager() {
		return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.NOT_PROJECT_MANAGER,
				"Only the project manager can do this.");
	}

	static ApiException memberNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.MEMBER_NOT_FOUND, "Member not found.");
	}

	static ApiException alreadyMember() {
		return new ApiException(HttpStatus.CONFLICT, ErrorCode.ALREADY_MEMBER, "This person is already a member.");
	}

	static ApiException projectManagerNotRemovable() {
		return new ApiException(HttpStatus.CONFLICT, ErrorCode.PROJECT_MANAGER_NOT_REMOVABLE,
				"The project manager can't be removed from the project.");
	}

}
