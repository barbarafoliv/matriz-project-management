package com.matriz.tasks;

import com.matriz.accounts.User;

/**
 * A user shown on a task (docs/openapi.yaml, UserSummary).
 */
public record UserSummary(long id, String name) {

	static UserSummary of(User user) {
		return new UserSummary(user.getId(), user.getName());
	}

}
