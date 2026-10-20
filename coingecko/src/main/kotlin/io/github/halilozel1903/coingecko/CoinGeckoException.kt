package io.github.halilozel1903.coingecko

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Every failure of [CoinGeckoClient]. Catch the subclasses you care about, or all of them. */
public sealed class CoinGeckoException(
    /** The requested url. */
    public val url: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** The API answered 404: there is no coin with that id, for example. */
    public class NotFound(url: String, public val body: String? = null) :
        CoinGeckoException(url, "Not found: $url" + describe(body))

    /**
     * The API answered 429 and the retries of the [RetryPolicy] did not help, or the server asked for a longer wait
     * than the policy allows. [retryAfterMillis] is the wait the server asked for, when it sent `Retry-After`.
     */
    public class RateLimited(url: String, public val retryAfterMillis: Long?, public val body: String? = null) :
        CoinGeckoException(
            url,
            "Rate limited by CoinGecko for $url" +
                (retryAfterMillis?.let { " (retry after ${(it + 999) / 1_000} s)" } ?: "") + describe(body),
        )

    /**
     * The API answered with another unsuccessful [status]: 400 for a bad parameter, 401 for an invalid or missing key,
     * 403 when the request was refused, 5xx after the retries.
     */
    public class Http(url: String, public val status: Int, public val body: String? = null) :
        CoinGeckoException(url, "HTTP $status for $url" + describe(body)) {
        /** The API's own message from the body (`status.error_message` or `error`), if there is one. */
        public val errorMessage: String? get() = apiErrorMessage(body)
    }

    /** The request failed before a response arrived (offline, timeout, DNS). */
    public class Network(url: String, cause: Throwable) :
        CoinGeckoException(url, "Request failed for $url: ${cause.message ?: cause::class.simpleName}", cause)

    /** The response was not the JSON this library expects. */
    public class Decoding(url: String, cause: Throwable) :
        CoinGeckoException(url, "Could not decode the response of $url: ${cause.message}", cause)

    internal companion object {
        private fun describe(body: String?): String =
            (apiErrorMessage(body) ?: body?.takeIf { it.isNotBlank() }?.take(200))?.let { ": $it" } ?: ""

        /** Reads `{"status":{"error_message":"..."}}` and `{"error":"..."}`, the two error shapes of the API. */
        fun apiErrorMessage(body: String?): String? {
            if (body.isNullOrBlank()) return null
            val obj = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return null
            val status = obj["status"] as? JsonObject
            return (status?.get("error_message") as? JsonPrimitive)?.contentOrNull
                ?: (obj["error"] as? JsonPrimitive)?.contentOrNull
        }
    }
}
