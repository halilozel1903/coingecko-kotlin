package io.github.halilozel1903.coingecko.transport

/**
 * Fetches the body of an API url. The client uses a Ktor based transport by default, which sends the API key and
 * retries rate limited requests; supply your own through `CoinGeckoConfig.transport` to serve responses from files,
 * a cache or another HTTP stack.
 *
 * Throw [io.github.halilozel1903.coingecko.CoinGeckoException] subclasses for HTTP errors; other exceptions are
 * reported as [io.github.halilozel1903.coingecko.CoinGeckoException.Network].
 */
public fun interface CoinGeckoTransport {
    public suspend fun get(url: String): String
}
