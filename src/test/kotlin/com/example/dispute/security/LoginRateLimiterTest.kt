package com.example.dispute.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class LoginRateLimiterTest {

    private val clock = MutableClock(Instant.parse("2026-10-22T08:00:00Z"))
    private val limiter = LoginRateLimiter(maxFailures = 2, window = Duration.ofMinutes(10), clock = clock)

    @Test
    fun `a client is allowed until it reaches the failure limit`() {
        limiter.recordFailure("ip")
        assertNull(limiter.retryAfter("ip"))

        limiter.recordFailure("ip")
        assertEquals(Duration.ofMinutes(10), limiter.retryAfter("ip"))
    }

    @Test
    fun `retry-after shrinks as the window elapses`() {
        limiter.recordFailure("ip")
        limiter.recordFailure("ip")

        clock.now = clock.now.plus(Duration.ofMinutes(4))

        assertEquals(Duration.ofMinutes(6), limiter.retryAfter("ip"))
    }

    @Test
    fun `the block lifts once the window has passed`() {
        limiter.recordFailure("ip")
        limiter.recordFailure("ip")

        clock.now = clock.now.plus(Duration.ofMinutes(10))

        assertNull(limiter.retryAfter("ip"))
    }

    @Test
    fun `a blocked client does not affect other clients`() {
        limiter.recordFailure("attacker")
        limiter.recordFailure("attacker")

        assertNull(limiter.retryAfter("other"))
    }

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }
}
