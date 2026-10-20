package io.github.halilozel1903.coingecko

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.random.Random

/**
 * How the client retries a request that was rate limited (HTTP 429), failed on the server (5xx) or did not reach the
 * server at all.
 *
 * - A `Retry-After` header (seconds or an HTTP date) is honored when [respectRetryAfter] is on. When the server asks
 *   for a longer wait than [maxDelayMillis], the client does not sleep that long: it throws
 *   [CoinGeckoException.RateLimited] right away, with the requested wait.
 * - Without the header the wait doubles from [initialDelayMillis] on every attempt, capped at [maxDelayMillis], plus
 *   up to [jitterRatio] of random jitter so that clients on a shared network do not retry in lockstep.
 *
 * Other errors (400, 401, 403, 404) are never retried.
 */
public data class RetryPolicy(
    /** Retries after the first attempt. `0` turns retries off. */
    public val maxRetries: Int = 3,
    /** The first backoff wait, in milliseconds. */
    public val initialDelayMillis: Long = 2_000,
    /** The longest wait between two attempts, in milliseconds, for backoff and for `Retry-After`. */
    public val maxDelayMillis: Long = 60_000,
    /** Whether to wait as long as a `Retry-After` header asks. */
    public val respectRetryAfter: Boolean = true,
    /** Random extra wait added to backoff, as a share of the wait (0.0 to 1.0). Not added to `Retry-After`. */
    public val jitterRatio: Double = 0.2,
) {
    init {
        require(maxRetries >= 0) { "maxRetries must not be negative, was $maxRetries" }
        require(initialDelayMillis >= 0) { "initialDelayMillis must not be negative, was $initialDelayMillis" }
        require(maxDelayMillis >= initialDelayMillis) { "maxDelayMillis must be at least initialDelayMillis" }
        require(jitterRatio in 0.0..1.0) { "jitterRatio must be between 0 and 1, was $jitterRatio" }
    }

    /**
     * The exponential backoff before retry number [attempt] (0 for the first retry), with jitter from [random].
     */
    public fun backoffMillis(attempt: Int, random: Random = Random.Default): Long {
        require(attempt >= 0) { "attempt must not be negative, was $attempt" }
        val base = if (attempt >= 30) maxDelayMillis else (initialDelayMillis shl attempt).coerceAtMost(maxDelayMillis)
        val jitter = if (jitterRatio == 0.0 || base == 0L) 0L else (base * jitterRatio * random.nextDouble()).toLong()
        return (base + jitter).coerceAtMost(maxDelayMillis)
    }

    /**
     * What to do before retry number [attempt] after a 429 or 5xx response with the given `Retry-After` header:
     * the wait in milliseconds, or `null` when the server asks for longer than [maxDelayMillis] and the request
     * should fail instead.
     */
    public fun delayMillis(
        attempt: Int,
        retryAfter: String?,
        nowMillis: Long = System.currentTimeMillis(),
        random: Random = Random.Default,
    ): Long? {
        val requested = if (respectRetryAfter) parseRetryAfter(retryAfter, nowMillis) else null
        return when {
            requested == null -> backoffMillis(attempt, random)
            requested > maxDelayMillis -> null
            else -> requested
        }
    }

    public companion object {
        /** No retries: every failure is thrown at once. */
        public val None: RetryPolicy = RetryPolicy(maxRetries = 0)

        /**
         * The wait a `Retry-After` header asks for, in milliseconds: either delay-seconds (`"30"`) or an HTTP date
         * (`"Sat, 17 Oct 2026 09:41:00 GMT"`) relative to [nowMillis]. `null` when the header is missing or invalid.
         */
        public fun parseRetryAfter(value: String?, nowMillis: Long = System.currentTimeMillis()): Long? {
            val text = value?.trim().orEmpty()
            if (text.isEmpty()) return null
            text.toLongOrNull()?.let { seconds -> return if (seconds < 0) null else seconds * 1_000 }
            text.toDoubleOrNull()?.let { seconds -> return if (seconds < 0) null else (seconds * 1_000).toLong() }
            return try {
                val at = ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
                (at - nowMillis).coerceAtLeast(0)
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }
}
