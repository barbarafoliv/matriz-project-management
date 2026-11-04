package com.matriz.common.time;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The clock used by all modules for created and updated times, and for "today" (late tasks).
 *
 * <p>Its zone is the server's, because "late" uses the server's local date, which is the
 * user's (docs/database.md, ADR 0006). Times (instants) don't depend on the zone.
 *
 * <p>It ticks in microseconds, the precision PostgreSQL stores (TIMESTAMPTZ), so a response
 * shows exactly the time that was saved: the system clock can be finer (100 ns on Windows).
 */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.tick(Clock.systemDefaultZone(), Duration.ofNanos(1_000));
	}

}
