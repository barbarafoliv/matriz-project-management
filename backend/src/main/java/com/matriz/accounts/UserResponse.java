package com.matriz.accounts;

/**
 * The signed-in user (docs/openapi.yaml, User).
 */
record UserResponse(long id, String name, String email) {

	static UserResponse of(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail());
	}

}
