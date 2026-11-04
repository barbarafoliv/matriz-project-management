package com.matriz.accounts;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

/**
 * The signed-in user, kept in the server-side session. Other modules use {@link #id()} to
 * know who is making a request; it never comes from the request body.
 */
public record AuthenticatedUser(long id, String email) implements Principal, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return this.email;
	}

}
