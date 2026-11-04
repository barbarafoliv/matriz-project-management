package com.matriz.projects;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /projects/{projectId}/members} (docs/openapi.yaml, AddMemberRequest).
 */
record AddMemberRequest(
		@NotBlank(message = "Email must not be empty.")
		@Email(message = "Enter a valid email address.")
		@Size(max = 254, message = "Email must be at most 254 characters.") String email) {

}
