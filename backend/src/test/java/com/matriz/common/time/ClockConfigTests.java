package com.matriz.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/**
 * The shared clock: times in microseconds, like PostgreSQL stores them, and "today" in the
 * server's local date, which is the user's (FR-08, EC-05, docs/openapi.yaml Task.late,
 * docs/database.md).
 */
class ClockConfigTests {

	private final Clock clock = new ClockConfig().clock();

	@Test
	void usesTheServersLocalZoneForToday() {
		assertThat(this.clock.getZone()).isEqualTo(ZoneId.systemDefault());
	}

	@Test
	void ticksInMicroseconds() {
		for (int i = 0; i < 1000; i++) {
			assertThat(this.clock.instant().getNano() % 1_000).isZero();
		}
	}

}
