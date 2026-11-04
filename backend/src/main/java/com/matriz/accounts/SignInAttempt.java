package com.matriz.accounts;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Failed sign-ins in a row for one account's email, in lower case (NFR-02).
 */
@Entity
@Table(name = "sign_in_attempt")
class SignInAttempt {

	@Id
	@Column(length = 254)
	private String email;

	@Column(name = "failed_count", nullable = false)
	private int failedCount;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	protected SignInAttempt() {
	}

	SignInAttempt(String email) {
		this.email = email;
	}

	int getFailedCount() {
		return this.failedCount;
	}

	Instant getLockedUntil() {
		return this.lockedUntil;
	}

	boolean isLockedAt(Instant now) {
		return this.lockedUntil != null && this.lockedUntil.isAfter(now);
	}

	void recordFailure(int maxFailures, Instant lockUntil) {
		this.failedCount++;
		if (this.failedCount >= maxFailures) {
			this.lockedUntil = lockUntil;
		}
	}

	void reset() {
		this.failedCount = 0;
		this.lockedUntil = null;
	}

}
