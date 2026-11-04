package com.matriz.accounts;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account data for the signed-in user (US-01).
 */
@Service
public class AccountService {

	private final UserRepository users;

	AccountService(UserRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public Optional<User> findUser(long id) {
		return this.users.findById(id);
	}

}
