package com.matriz.accounts;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.matriz.common.web.ApiException;
import com.matriz.common.web.ErrorCode;
import com.matriz.projects.ProjectService;

/**
 * Deletes an account and its personal data (FR-14, US-09, EC-06).
 *
 * <p>The user is deleted, and the database removes their memberships and assignments and
 * sets the author of their comments to null (docs/database.md, section 4). Their failed
 * sign-in record is deleted too, and all their sessions once the deletion is committed. The
 * email can then be used by a new account.
 */
@Service
public class AccountDeletionService {

	private final UserRepository users;

	private final SignInAttemptRepository attempts;

	private final ProjectService projects;

	private final FindByIndexNameSessionRepository<? extends Session> sessions;

	AccountDeletionService(UserRepository users, SignInAttemptRepository attempts, ProjectService projects,
			FindByIndexNameSessionRepository<? extends Session> sessions) {
		this.users = users;
		this.attempts = attempts;
		this.projects = projects;
		this.sessions = sessions;
	}

	/**
	 * @throws ApiException {@code ACCOUNT_MANAGES_PROJECTS} while the user is the project
	 * manager of a project (EC-06)
	 */
	@Transactional
	public void deleteAccount(long userId) {
		User user = this.users.findById(userId)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED,
					"Sign in to continue."));
		if (this.projects.managesAnyProject(userId)) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.ACCOUNT_MANAGES_PROJECTS,
					"Delete the projects you manage before deleting your account.");
		}
		this.attempts.deleteById(user.getEmail().toLowerCase(Locale.ROOT));
		this.users.delete(user);
		// Sign the user out everywhere (their sessions are stored under their email), but only
		// once the deletion is committed: Spring Session commits its changes on its own.
		String email = user.getEmail();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				AccountDeletionService.this.sessions.findByPrincipalName(email)
					.keySet()
					.forEach(AccountDeletionService.this.sessions::deleteById);
			}
		});
	}

}
