package io.github.halilozel1903.coingecko

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetryTest {

    private val rateLimited = fixture("rate-limited.json")
    private val ok = Reply(HttpStatusCode.OK, fixture("ping.json"))

    @Test
    fun waitsAsLongAsRetryAfterAsks() = runTest {
        MockApi(
            script = listOf(Reply(HttpStatusCode.TooManyRequests, rateLimited, retryAfter = "7"), ok),
            configure = { retryPolicy = RetryPolicy(maxRetries = 3, jitterRatio = 0.0) },
        ).use { api ->
            assertEquals("(V3) To the Moon!", api.client.ping().geckoSays)
            assertEquals(2, api.requests.size)
            assertEquals(7_000, currentTime)
        }
    }

    @Test
    fun backsOffExponentiallyWithoutRetryAfter() = runTest {
        MockApi(
            script = listOf(
                Reply(HttpStatusCode.TooManyRequests, rateLimited),
                Reply(HttpStatusCode.ServiceUnavailable, ""),
                ok,
            ),
            configure = { retryPolicy = RetryPolicy(maxRetries = 3, initialDelayMillis = 1_000, jitterRatio = 0.0) },
        ).use { api ->
            api.client.ping()
            assertEquals(3, api.requests.size)
            assertEquals(1_000 + 2_000, currentTime)
        }
    }

    @Test
    fun givesUpWithRateLimitedAfterTheRetries() = runTest {
        MockApi(
            script = listOf(Reply(HttpStatusCode.TooManyRequests, rateLimited, retryAfter = "2")),
            configure = { retryPolicy = RetryPolicy(maxRetries = 2, jitterRatio = 0.0) },
        ).use { api ->
            val error = assertFailsWith<CoinGeckoException.RateLimited> { api.client.ping() }
            assertEquals(3, api.requests.size)
            assertEquals(2_000, error.retryAfterMillis)
            assertTrue(error.message!!.contains("exceeded the Rate Limit"))
        }
    }

    @Test
    fun doesNotSleepThroughALongRetryAfter() = runTest {
        MockApi(
            script = listOf(Reply(HttpStatusCode.TooManyRequests, rateLimited, retryAfter = "3600")),
            configure = { retryPolicy = RetryPolicy(maxRetries = 3) },
        ).use { api ->
            val error = assertFailsWith<CoinGeckoException.RateLimited> { api.client.ping() }
            assertEquals(1, api.requests.size)
            assertEquals(3_600_000, error.retryAfterMillis)
            assertEquals(0, currentTime)
        }
    }

    @Test
    fun doesNotRetryClientErrors() = runTest {
        MockApi(
            script = listOf(Reply(HttpStatusCode.Unauthorized, """{"status":{"error_code":10002,"error_message":"API Key Missing"}}""")),
            configure = { retryPolicy = RetryPolicy(maxRetries = 3) },
        ).use { api ->
            val error = assertFailsWith<CoinGeckoException.Http> { api.client.global() }
            assertEquals(401, error.status)
            assertEquals("API Key Missing", error.errorMessage)
            assertEquals(1, api.requests.size)
        }
    }

    @Test
    fun reportsServerErrorsAfterRetries() = runTest {
        MockApi(
            script = listOf(Reply(HttpStatusCode.BadGateway, "")),
            configure = { retryPolicy = RetryPolicy(maxRetries = 1, initialDelayMillis = 500, jitterRatio = 0.0) },
        ).use { api ->
            assertEquals(502, assertFailsWith<CoinGeckoException.Http> { api.client.ping() }.status)
            assertEquals(2, api.requests.size)
            assertEquals(500, currentTime)
        }
    }

    @Test
    fun parsesRetryAfter() {
        val now = 1_792_228_800_000L // Sat, 17 Oct 2026 09:20:00 GMT
        assertEquals(30_000, RetryPolicy.parseRetryAfter("30", now))
        assertEquals(1_500, RetryPolicy.parseRetryAfter(" 1.5 ", now))
        assertEquals(60_000, RetryPolicy.parseRetryAfter("Sat, 17 Oct 2026 09:21:00 GMT", now))
        assertEquals(0, RetryPolicy.parseRetryAfter("Sat, 17 Oct 2026 09:00:00 GMT", now))
        assertNull(RetryPolicy.parseRetryAfter("soon", now))
        assertNull(RetryPolicy.parseRetryAfter("-5", now))
        assertNull(RetryPolicy.parseRetryAfter(null, now))
    }

    @Test
    fun capsBackoffAndAddsBoundedJitter() {
        val policy = RetryPolicy(initialDelayMillis = 1_000, maxDelayMillis = 10_000, jitterRatio = 0.5)
        val random = Random(42)
        repeat(50) {
            val first = policy.backoffMillis(0, random)
            assertTrue(first in 1_000..1_500, "was $first")
        }
        assertEquals(10_000, policy.backoffMillis(10, random))
        assertEquals(10_000, policy.backoffMillis(40, random))
        val noJitter = policy.copy(jitterRatio = 0.0)
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 10_000L), (0..4).map { noJitter.backoffMillis(it) })
    }

    @Test
    fun decidesBetweenWaitingAndGivingUp() {
        val policy = RetryPolicy(initialDelayMillis = 1_000, maxDelayMillis = 60_000, jitterRatio = 0.0)
        assertEquals(20_000, policy.delayMillis(0, "20"))
        assertNull(policy.delayMillis(0, "61"))
        assertEquals(4_000, policy.delayMillis(2, null))
        assertEquals(1_000, policy.copy(respectRetryAfter = false).delayMillis(0, "61"))
    }
}
