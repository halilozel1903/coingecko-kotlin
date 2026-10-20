package io.github.halilozel1903.coingecko

import io.github.halilozel1903.coingecko.transport.CoinGeckoTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

/** Settings for [CoinGeckoClient], set in its builder block. Every setting is optional; no API key is required. */
public class CoinGeckoConfig internal constructor() {
    /**
     * A free Demo API key from the CoinGecko developer dashboard. It is sent as the `x-cg-demo-api-key` header to the
     * public API root and gives a stable rate limit. `null` (the default) uses the keyless public tier.
     */
    public var demoApiKey: String? = null

    /**
     * A paid Pro API key. It is sent as the `x-cg-pro-api-key` header and switches the API root to
     * [CoinGeckoClient.PRO_BASE_URL]. Set either this or [demoApiKey], not both.
     */
    public var proApiKey: String? = null

    /** Overrides the API root, for example for a proxy. `null` picks the public or the Pro root from the key. */
    public var baseUrl: String? = null

    /**
     * The Ktor engine. `null` uses CIO. Pass `OkHttp.create()` (add `io.ktor:ktor-client-okhttp`) to share your
     * app's OkHttp stack, or a `MockEngine` in tests. The client does not close an engine you pass.
     */
    public var engine: HttpClientEngine? = null

    /** A ready Ktor client to use as is, instead of [engine]. [requestTimeoutMillis] then does not apply. */
    public var httpClient: HttpClient? = null

    /**
     * Replaces HTTP entirely, for example to serve recorded responses. A custom transport is responsible for its own
     * headers and retries; [demoApiKey], [proApiKey] and [retryPolicy] do not apply to it.
     */
    public var transport: CoinGeckoTransport? = null

    /** Sent with every request. */
    public var userAgent: String = "coingecko-kotlin/1.0.0 (+https://github.com/halilozel1903/coingecko-kotlin)"

    /** Timeout of one request, in milliseconds. */
    public var requestTimeoutMillis: Long = 30_000

    /**
     * How rate limited (429), failed (5xx) and unreachable requests are retried. The default retries three times,
     * waits as long as the `Retry-After` header asks, and gives up at once when that is longer than a minute.
     */
    public var retryPolicy: RetryPolicy = RetryPolicy()
}

/** Which CoinGecko plan the client talks to, from the configured key. */
public enum class ApiPlan {
    /** No key: the keyless public tier, with a low and shared rate limit. */
    Public,

    /** A Demo key, sent as `x-cg-demo-api-key`. */
    Demo,

    /** A Pro key, sent as `x-cg-pro-api-key` to `pro-api.coingecko.com`. */
    Pro,
}
