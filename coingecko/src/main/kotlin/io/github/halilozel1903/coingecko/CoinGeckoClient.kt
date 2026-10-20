package io.github.halilozel1903.coingecko

import io.github.halilozel1903.coingecko.model.CoinDetail
import io.github.halilozel1903.coingecko.model.GlobalData
import io.github.halilozel1903.coingecko.model.GlobalResponse
import io.github.halilozel1903.coingecko.model.MarketChart
import io.github.halilozel1903.coingecko.model.MarketCoin
import io.github.halilozel1903.coingecko.model.Ping
import io.github.halilozel1903.coingecko.model.SearchResult
import io.github.halilozel1903.coingecko.model.SimplePrice
import io.github.halilozel1903.coingecko.model.Trending
import io.github.halilozel1903.coingecko.transport.CoinGeckoTransport
import io.github.halilozel1903.coingecko.transport.KtorTransport
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.net.URLEncoder

/**
 * A coroutine client for the [CoinGecko API v3](https://docs.coingecko.com/v3.0.1/reference/introduction):
 * prices, markets, coin details, price charts, search, trending coins and global market data.
 *
 * ```kotlin
 * val client = CoinGeckoClient()                       // keyless public tier
 * val top = client.markets { perPage = 20; sparkline = true }
 * val btc = client.coin("bitcoin")
 * val week = client.marketChart("bitcoin", vsCurrency = "usd", days = 7)
 * ```
 *
 * No key is required. With a free Demo key (`demoApiKey`) or a Pro key (`proApiKey`) the client sends the matching
 * header, and a Pro key also switches to the Pro API root. Rate limited requests (429) are retried after the wait the
 * `Retry-After` header asks for, see [RetryPolicy].
 *
 * Every function is main-safe and throws a [CoinGeckoException] on failure. Close the client when you no longer need
 * it.
 */
public class CoinGeckoClient(configure: CoinGeckoConfig.() -> Unit = {}) : AutoCloseable {

    private val config = CoinGeckoConfig().apply(configure)

    /** The plan picked from the configured key. */
    public val plan: ApiPlan

    /** The API root, always ending with a slash. */
    public val baseUrl: String

    private val transport: CoinGeckoTransport

    init {
        val demo = config.demoApiKey?.trim()?.takeIf { it.isNotEmpty() }
        val pro = config.proApiKey?.trim()?.takeIf { it.isNotEmpty() }
        require(demo == null || pro == null) { "Set either demoApiKey or proApiKey, not both" }
        plan = when {
            pro != null -> ApiPlan.Pro
            demo != null -> ApiPlan.Demo
            else -> ApiPlan.Public
        }
        val root = config.baseUrl ?: if (plan == ApiPlan.Pro) PRO_BASE_URL else DEFAULT_BASE_URL
        baseUrl = if (root.endsWith('/')) root else "$root/"
        val headers = buildMap<String, String> {
            demo?.let { put(DEMO_KEY_HEADER, it) }
            pro?.let { put(PRO_KEY_HEADER, it) }
        }
        transport = config.transport ?: KtorTransport(
            httpClient = config.httpClient,
            engine = config.engine,
            userAgent = config.userAgent,
            extraHeaders = headers,
            requestTimeoutMillis = config.requestTimeoutMillis,
            retryPolicy = config.retryPolicy,
        )
    }

    // region Endpoints

    /** `GET /ping`: whether the API is up. */
    public suspend fun ping(): Ping = fetch(url("ping"), Ping.serializer())

    /**
     * `GET /simple/price`: the price of each coin of [ids] in each of [vsCurrencies], keyed by coin id. Coins the API
     * does not know are missing from the result.
     */
    public suspend fun simplePrice(
        ids: List<String>,
        vsCurrencies: List<String>,
        includeMarketCap: Boolean = false,
        include24hVolume: Boolean = false,
        include24hChange: Boolean = false,
        includeLastUpdatedAt: Boolean = false,
        precision: String? = null,
    ): Map<String, SimplePrice> {
        require(ids.isNotEmpty()) { "ids must not be empty" }
        require(vsCurrencies.isNotEmpty()) { "vsCurrencies must not be empty" }
        requireValidPrecision(precision)
        val params = buildList<Pair<String, String>> {
            add("ids" to ids.joinToString(",") { it.trim() })
            add("vs_currencies" to vsCurrencies.joinToString(",") { it.trim().lowercase() })
            if (includeMarketCap) add("include_market_cap" to "true")
            if (include24hVolume) add("include_24hr_vol" to "true")
            if (include24hChange) add("include_24hr_change" to "true")
            if (includeLastUpdatedAt) add("include_last_updated_at" to "true")
            precision?.let { add("precision" to it) }
        }
        val url = url("simple/price", params)
        val root = fetch(url, JsonObject.serializer())
        return try {
            root.mapValues { (id, value) -> parseSimplePrice(id, value as JsonObject) }
        } catch (e: ClassCastException) {
            throw CoinGeckoException.Decoding(url, e)
        }
    }

    /** `GET /simple/price` for one coin in one currency: just the price, or `null` when the API does not know it. */
    public suspend fun price(id: String, vsCurrency: String = "usd"): Double? =
        simplePrice(listOf(id), listOf(vsCurrency))[id]?.price(vsCurrency)

    /** `GET /coins/markets` with [query]: coins with price, market cap, volume and changes, one page at a time. */
    public suspend fun markets(query: MarketsQuery = MarketsQuery()): List<MarketCoin> =
        fetch(url("coins/markets", query.parameters()), ListSerializer(MarketCoin.serializer()))

    /** `GET /coins/markets` with a query built in [block]. */
    public suspend fun markets(block: MarketsQuery.Builder.() -> Unit): List<MarketCoin> = markets(marketsQuery(block))

    /** `GET /coins/{id}` with the trimmed [options] by default (market data and the English description only). */
    public suspend fun coin(id: String, options: CoinOptions = CoinOptions()): CoinDetail =
        fetch(url("coins/${pathSegment(id)}", options.parameters()), CoinDetail.serializer())

    /** `GET /coins/{id}/market_chart`: prices, market caps and volumes of the last [days] in [vsCurrency]. */
    public suspend fun marketChart(
        id: String,
        vsCurrency: String = "usd",
        days: ChartDays = ChartDays.Week,
        interval: ChartInterval? = null,
        precision: String? = null,
    ): MarketChart {
        requireValidPrecision(precision)
        val params = buildList<Pair<String, String>> {
            add("vs_currency" to vsCurrency.trim().lowercase())
            add("days" to days.apiValue)
            interval?.let { add("interval" to it.apiValue) }
            precision?.let { add("precision" to it) }
        }
        return fetch(url("coins/${pathSegment(id)}/market_chart", params), MarketChart.serializer())
    }

    /** `GET /coins/{id}/market_chart` for the last [days] days. */
    public suspend fun marketChart(id: String, vsCurrency: String, days: Int): MarketChart =
        marketChart(id, vsCurrency, ChartDays.of(days))

    /** `GET /search`: coins, exchanges, categories and NFTs matching [query]. */
    public suspend fun search(query: String): SearchResult {
        require(query.isNotBlank()) { "query must not be blank" }
        return fetch(url("search", listOf("query" to query.trim())), SearchResult.serializer())
    }

    /** `GET /search/trending`: the most searched coins, NFTs and categories of the last 24 hours. */
    public suspend fun trending(): Trending = fetch(url("search/trending"), Trending.serializer())

    /** `GET /global`: total market cap and volume, dominance and counts of the whole market. */
    public suspend fun global(): GlobalData = fetch(url("global"), GlobalResponse.serializer()).data

    // endregion

    // region Generic access

    /** Loads and decodes any url with [deserializer], through the configured transport and retries. */
    public suspend fun <T> fetch(url: String, deserializer: DeserializationStrategy<T>): T {
        val body = try {
            transport.get(url)
        } catch (e: CoinGeckoException) {
            throw e
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw CoinGeckoException.Network(url, e)
        }
        return try {
            json.decodeFromString(deserializer, body)
        } catch (e: SerializationException) {
            throw CoinGeckoException.Decoding(url, e)
        } catch (e: IllegalArgumentException) {
            throw CoinGeckoException.Decoding(url, e)
        }
    }

    /** The url of [path] below [baseUrl] with the [parameters] in order, encoded. */
    public fun url(path: String, parameters: List<Pair<String, String>> = emptyList()): String {
        val query = parameters.joinToString("&") { (name, value) -> "$name=${encode(value)}" }
        return baseUrl + path.trimStart('/') + if (query.isEmpty()) "" else "?$query"
    }

    // endregion

    /** Releases the HTTP client this instance created. */
    override fun close() {
        (transport as? AutoCloseable)?.close()
    }

    public companion object {
        /** The public API root, keyless or with a Demo key. */
        public const val DEFAULT_BASE_URL: String = "https://api.coingecko.com/api/v3/"

        /** The Pro API root, used with a Pro key. */
        public const val PRO_BASE_URL: String = "https://pro-api.coingecko.com/api/v3/"

        /** The header of a Demo key. */
        public const val DEMO_KEY_HEADER: String = "x-cg-demo-api-key"

        /** The header of a Pro key. */
        public const val PRO_KEY_HEADER: String = "x-cg-pro-api-key"

        internal val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

        private fun encode(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20").replace("%2C", ",")

        private fun pathSegment(id: String): String {
            require(id.isNotBlank()) { "id must not be blank" }
            return URLEncoder.encode(id.trim(), "UTF-8").replace("+", "%20")
        }

        internal fun parseSimplePrice(id: String, values: JsonObject): SimplePrice {
            val prices = LinkedHashMap<String, Double>()
            val caps = LinkedHashMap<String, Double>()
            val volumes = LinkedHashMap<String, Double>()
            val changes = LinkedHashMap<String, Double>()
            var updated: Long? = null
            for ((key, element) in values) {
                val primitive = element as? JsonPrimitive ?: continue
                when {
                    key == "last_updated_at" -> updated = primitive.longOrNull ?: primitive.doubleOrNull?.toLong()
                    key.endsWith("_market_cap") -> primitive.doubleOrNull?.let { caps[key.removeSuffix("_market_cap")] = it }
                    key.endsWith("_24h_vol") -> primitive.doubleOrNull?.let { volumes[key.removeSuffix("_24h_vol")] = it }
                    key.endsWith("_24h_change") -> primitive.doubleOrNull?.let { changes[key.removeSuffix("_24h_change")] = it }
                    else -> primitive.doubleOrNull?.let { prices[key] = it }
                }
            }
            return SimplePrice(id, prices, caps, volumes, changes, updated)
        }
    }
}
