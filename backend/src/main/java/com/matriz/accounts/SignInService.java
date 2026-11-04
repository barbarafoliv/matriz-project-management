package com.matriz.accounts;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks email and password, and limits failed sign-ins (US-01, EC-02, NFR-02).
 *
 * <p>After 5 failed attempts in a row for an account, sign-in is blocked for 15 minutes,
 * even with the right password. A successful sign-in clears the count. Failures for emails
 * that have no account aren't counted.
 */
@Service
public class SignInService {

	static final int MAX_FAILED_ATTEMPTS = 5;

	static final Duration LOCK_DURATION = Duration.ofMinutes(15);

	private final UserRepository users;

	private final SignInAttemptRepository attempts;

	private final PasswordEncoder passwordEncoder;

	private final Clock clock;

	SignInService(UserRepository users, SignInAttemptRepository attempts, PasswordEncoder passwordEncoder,
			Optional<Clock> clock) {
		this.users = users;
		this.attempts = attempts;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock.orElseGet(Clock::systemUTC);
	}

	/**
	 * Returns the user if the email and password are correct.
	 * @throws InvalidCredentialsException if the email or password is wrong
	 * @throws TooManySignInAttemptsException if sign-in is blocked for this account
	 */
	@Transactional(noRollbackFor = { InvalidCredentialsException.class, TooManySignInAttemptsException.class })
	public User signIn(String email, String password) {
		User user = this.users.findByEmailIgnoreCase(email.trim()).orElseThrow(InvalidCredentialsException::new);
		String key = user.getEmail().toLowerCase(Locale.ROOT);
		Instant now = this.clock.instant();

		SignInAttempt attempt = this.attempts.findById(key).orElse(null);
		if (attempt != null && attempt.isLockedAt(now)) {
			throw new TooManySignInAttemptsException(Duration.between(now, attempt.getLockedUntil()));
		}
		if (attempt != null && attempt.getLockedUntil() != null) {
			attempt.reset();
		}

		if (!this.passwordEncoder.matches(password, user.getPasswordHash())) {
			SignInAttempt failed = (attempt != null) ? attempt : new SignInAttempt(key);
			failed.recordFailure(MAX_FAILED_ATTEMPTS, now.plus(LOCK_DURATION));
			this.attempts.save(failed);
			throw new InvalidCredentialsException();
		}

		if (attempt != null) {
			this.attempts.delete(attempt);
		}
		return user;
	}

}
