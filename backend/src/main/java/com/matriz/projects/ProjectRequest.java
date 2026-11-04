package com.matriz.projects;


import com.matriz.common.web.NotBlankText;
import com.matriz.common.web.TrimmedLength;

/**
 * Body of {@code POST /projects} and {@code PUT /projects/{projectId}} (docs/openapi.yaml,
 * ProjectRequest).
 */
record ProjectRequest(
		@NotBlankText(message = "Name must not be empty.")
		@TrimmedLength(min = 3, max = 100, message = "Name must be between 3 and 100 characters.") String name,
		@TrimmedLength(max = 1000, message = "Description must be at most 1000 characters.") String description) {

}
