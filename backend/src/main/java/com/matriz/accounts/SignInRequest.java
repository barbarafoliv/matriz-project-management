package com.matriz.accounts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.hibernate.validator.constraints.CodePointLength;

/**
 * Body of {@code POST /sessions} (docs/openapi.yaml, SignInRequest).
 */
record SignInRequest(
		@NotBlank(message = "Email must not be empty.")
		@Email(message = "Enter a valid email address.")
		@CodePointLength(max = 254, message = "Email must be at most 254 characters.") String email,
		@NotEmpty(message = "Password must not be empty.")
		@CodePointLength(max = 64, message = "Password must be at most 64 characters.") String password) {

	@Override
	public String toString() {
		return "SignInRequest[email=" + this.email + "]";
	}

}
