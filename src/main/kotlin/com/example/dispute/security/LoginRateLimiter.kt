package com.example.dispute.security

import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Counts failed login attempts per client in fixed windows. State is in memory, so it resets on restart and is not shared between instances.
 *
 * @property maxFailures the failures allowed within one window before further attempts are blocked
 * @property window the length of each window
 * @property clock the clock used to time windows
 */
class LoginRateLimiter(
    private val maxFailures: Int,
    private val window: Duration,
    private val clock: Clock,
) {
    private class Window(val startedAt: Long, val failures: Int)

    private val windows = ConcurrentHashMap<String, Window>()

    /**
     * Returns how long a client must wait before it may attempt login again.
     *
     * @param key the client identifier, such as its IP address
     * @return the time until the blocking window ends, or null when the client may attempt login
     */
    fun retryAfter(key: String): Duration? {
        val now = clock.millis()
        val current = windows[key]?.takeIf { isOpen(it, now) } ?: return null
        if (current.failures < maxFailures) return null
        return Duration.ofMillis(window.toMillis() - (now - current.startedAt))
    }

    /**
     * Records one failed login attempt for the client.
     *
     * @param key the client identifier, such as its IP address
     */
    fun recordFailure(key: String) {
        val now = clock.millis()
        windows.compute(key) { _, current ->
            val open = current?.takeIf { isOpen(it, now) }
            if (open == null) Window(now, 1) else Window(open.startedAt, open.failures + 1)
        }
        if (windows.size > MAX_TRACKED_CLIENTS) {
            windows.entries.removeIf { !isOpen(it.value, now) }
        }
    }

    private fun isOpen(entry: Window, now: Long): Boolean = now - entry.startedAt < window.toMillis()

    private companion object {
        const val MAX_TRACKED_CLIENTS = 10_000
    }
}
