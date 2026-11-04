package com.matriz.accounts;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account data, for the signed-in user (US-01) and for other modules that show users,
 * such as project members (US-03).
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

	/**
	 * Finds the account with this email, ignoring upper and lower case (EC-01).
	 */
	@Transactional(readOnly = true)
	public Optional<User> findUserByEmail(String email) {
		return this.users.findByEmailIgnoreCase(email.trim());
	}

	@Transactional(readOnly = true)
	public List<User> findUsers(Collection<Long> ids) {
		return this.users.findAllById(ids);
	}

}
