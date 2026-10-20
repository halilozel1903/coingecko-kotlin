package io.github.halilozel1903.coingecko.sample

import io.github.halilozel1903.coingecko.ChartDays
import io.github.halilozel1903.coingecko.CoinGeckoClient
import io.github.halilozel1903.coingecko.MarketsQuery
import io.github.halilozel1903.coingecko.PriceChangeWindow
import io.github.halilozel1903.coingecko.RetryPolicy
import io.github.halilozel1903.coingecko.model.CoinDetail
import io.github.halilozel1903.coingecko.model.GlobalData
import io.github.halilozel1903.coingecko.model.MarketChart
import io.github.halilozel1903.coingecko.model.MarketCoin
import io.github.halilozel1903.coingecko.model.SearchResult
import io.github.halilozel1903.coingecko.model.Trending
import io.ktor.client.engine.okhttp.OkHttp

/** The vs currencies the tracker offers. */
enum class Currency(val code: String) {
    Usd("usd"),
    Eur("eur"),
    Gbp("gbp"),
    Jpy("jpy"),
}

/** The chart ranges of the detail screen. */
enum class ChartRange(val label: String, val days: ChartDays) {
    Day("24h", ChartDays.Day),
    Week("7d", ChartDays.Week),
    Month("30d", ChartDays.Month),
    Year("1y", ChartDays.Year),
}

/**
 * The sample's only data source: the live CoinGecko API through [CoinGeckoClient], keyless by default. There is no cached,
 * bundled or fake data; when CoinGecko refuses a request the screen shows the error.
 */
class CryptoRepository(private val client: CoinGeckoClient) {

    suspend fun global(): GlobalData = client.global()

    suspend fun markets(currency: Currency, page: Int): List<MarketCoin> = client.markets(
        MarketsQuery(
            vsCurrency = currency.code,
            perPage = PAGE_SIZE,
            page = page,
            sparkline = true,
            priceChangePercentage = listOf(PriceChangeWindow.Hour, PriceChangeWindow.Day, PriceChangeWindow.Week),
        ),
    )

    suspend fun coin(id: String): CoinDetail = client.coin(id)

    suspend fun chart(id: String, currency: Currency, range: ChartRange): MarketChart =
        client.marketChart(id, currency.code, range.days)

    suspend fun trending(): Trending = client.trending()

    suspend fun search(query: String): SearchResult = client.search(query)

    companion object {
        const val PAGE_SIZE = 50
    }
}

/** The app's repositories, created on first use. */
object Repositories {
    private var live: CryptoRepository? = null
    private var scenes: CryptoRepository? = null

    private fun client(policy: RetryPolicy) = CoinGeckoClient {
        // Shares OkHttp with Coil. Keyless unless a Demo key was given at build time (COINGECKO_DEMO_API_KEY).
        engine = OkHttp.create()
        demoApiKey = BuildConfig.COINGECKO_DEMO_API_KEY.ifBlank { null }
        userAgent = "coingecko-kotlin-sample/1.0.0 (+https://github.com/halilozel1903/coingecko-kotlin)"
        retryPolicy = policy
    }

    @Synchronized
    fun live(): CryptoRepository = live ?: CryptoRepository(client(RetryPolicy())).also { live = it }

    /**
     * Screenshot scenes: CI runners share IP addresses, so the keyless rate limit can be hit by others. Retry a few
     * more times, starting slower, and still honor Retry-After up to a minute.
     */
    @Synchronized
    fun scenes(): CryptoRepository = scenes ?: CryptoRepository(
        client(RetryPolicy(maxRetries = 4, initialDelayMillis = 5_000, maxDelayMillis = 60_000)),
    ).also { scenes = it }
}
