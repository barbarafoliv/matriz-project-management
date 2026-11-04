package com.matriz.projects;

import com.matriz.accounts.User;

/**
 * A project member (docs/openapi.yaml, Member).
 */
public record MemberView(long userId, String name, String email, Role role) {

	MemberView(User user, Role role) {
		this(user.getId(), user.getName(), user.getEmail(), role);
	}

}
