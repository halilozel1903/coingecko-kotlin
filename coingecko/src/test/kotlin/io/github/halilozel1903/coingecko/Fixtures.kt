package io.github.halilozel1903.coingecko

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Reads `src/test/resources/fixtures/<name>`. The fixtures are hand-written in the shape of real v3 responses (field
 * names, nesting, nulls, image urls); the numbers are illustrative, not market data.
 */
internal fun fixture(name: String): String =
    requireNotNull(MockApi::class.java.getResource("/fixtures/$name")) { "Missing fixture $name" }.readText()

private const val BASE = CoinGeckoClient.DEFAULT_BASE_URL

/** Which fixture answers which path and query. Anything else is a 404 like the API's. */
internal val routes: Map<String, String> = mapOf(
    "ping" to "ping.json",
    "simple/price?ids=bitcoin,ethereum&vs_currencies=usd,eur&include_market_cap=true&include_24hr_vol=true" +
        "&include_24hr_change=true&include_last_updated_at=true" to "simple-price.json",
    "simple/price?ids=bitcoin&vs_currencies=usd" to "simple-price.json",
    "coins/markets?vs_currency=usd&order=market_cap_desc&per_page=3&page=1&sparkline=true" +
        "&price_change_percentage=1h,24h,7d" to "coins-markets.json",
    "coins/bitcoin?localization=false&tickers=false&market_data=true&community_data=false" +
        "&developer_data=false&sparkline=false" to "coin-bitcoin.json",
    "coins/bitcoin/market_chart?vs_currency=usd&days=7" to "market-chart-bitcoin-7.json",
    "search?query=bitcoin" to "search-bitcoin.json",
    "search/trending" to "trending.json",
    "global" to "global.json",
)

/** One scripted response. */
internal data class Reply(val status: HttpStatusCode, val body: String, val retryAfter: String? = null)

/**
 * A client on a Ktor [MockEngine]. Without [script] it serves the fixtures by url; with it, each request takes the
 * next reply (the last one repeats).
 */
internal class MockApi(
    private val script: List<Reply> = emptyList(),
    configure: CoinGeckoConfig.() -> Unit = {},
) : AutoCloseable {
    val requests: MutableList<HttpRequestData> = CopyOnWriteArrayList()

    val engine = MockEngine { request ->
        requests += request
        if (script.isNotEmpty()) {
            reply(script[(requests.size - 1).coerceAtMost(script.lastIndex)])
        } else {
            val path = request.url.toString().removePrefix(BASE).removePrefix(CoinGeckoClient.PRO_BASE_URL)
            val name = routes[path]
            if (name == null) {
                reply(Reply(HttpStatusCode.NotFound, fixture("coin-not-found.json")))
            } else {
                reply(Reply(HttpStatusCode.OK, fixture(name)))
            }
        }
    }

    private fun MockRequestHandleScope.reply(reply: Reply): HttpResponseData {
        val headers = if (reply.retryAfter != null) {
            headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.RetryAfter to listOf(reply.retryAfter))
        } else {
            headersOf(HttpHeaders.ContentType, "application/json")
        }
        return respond(reply.body, reply.status, headers)
    }

    val client = CoinGeckoClient {
        engine = this@MockApi.engine
        retryPolicy = RetryPolicy(maxRetries = 0)
        configure()
    }

    val urls: List<String> get() = requests.map { it.url.toString() }

    override fun close() {
        client.close()
        engine.close()
    }
}
