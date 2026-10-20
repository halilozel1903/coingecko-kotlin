package io.github.halilozel1903.coingecko.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * `GET /coins/{id}`: one coin's metadata and, with `market_data=true`, its market data in every vs currency.
 * Fields of the flags that were turned off are left at their defaults.
 */
@Serializable
public data class CoinDetail(
    public val id: String,
    /** The ticker in lowercase, such as `btc`. */
    public val symbol: String = "",
    public val name: String = "",
    @SerialName("web_slug") public val webSlug: String? = null,
    /** The chain a token lives on, such as `ethereum`; `null` for native coins. */
    @SerialName("asset_platform_id") public val assetPlatformId: String? = null,
    /** Contract addresses by platform id. Native coins have an entry with an empty key. */
    public val platforms: Map<String, String?> = emptyMap(),
    @SerialName("block_time_in_minutes") public val blockTimeInMinutes: Double? = null,
    @SerialName("hashing_algorithm") public val hashingAlgorithm: String? = null,
    /** Category names, such as `"Layer 1 (L1)"`. */
    public val categories: List<String?> = emptyList(),
    /** Descriptions by language code. Only `en` without `localization=true`. The text may contain HTML links. */
    public val description: Map<String, String?> = emptyMap(),
    public val links: CoinLinks = CoinLinks(),
    public val image: CoinImage = CoinImage(),
    @SerialName("country_origin") public val countryOrigin: String? = null,
    /** `YYYY-MM-DD`, for coins that have one. */
    @SerialName("genesis_date") public val genesisDate: String? = null,
    @SerialName("sentiment_votes_up_percentage") public val sentimentVotesUpPercentage: Double? = null,
    @SerialName("sentiment_votes_down_percentage") public val sentimentVotesDownPercentage: Double? = null,
    @SerialName("watchlist_portfolio_users") public val watchlistPortfolioUsers: Long? = null,
    @SerialName("market_cap_rank") public val marketCapRank: Int? = null,
    /** With `market_data=true`. */
    @SerialName("market_data") public val marketData: CoinMarketData? = null,
    @SerialName("last_updated") public val lastUpdated: String? = null,
) {
    /** The English description as plain text: HTML tags removed, entities decoded, whitespace tidied. */
    public val descriptionText: String get() = htmlToText(description["en"].orEmpty())

    /** The first homepage that is not empty. */
    public val homepage: String? get() = links.homepage.firstOrNull { !it.isNullOrBlank() }

    /** Category names without empty entries. */
    public val categoryNames: List<String> get() = categories.filterNotNull().filter { it.isNotBlank() }

    /** [lastUpdated] as an [Instant]. */
    public val lastUpdatedInstant: Instant? get() = lastUpdated.toInstantOrNull()
}

/** `links` of a coin. Lists may hold empty strings, as the API sends them. */
@Serializable
public data class CoinLinks(
    public val homepage: List<String?> = emptyList(),
    public val whitepaper: String? = null,
    @SerialName("blockchain_site") public val blockchainSite: List<String?> = emptyList(),
    @SerialName("official_forum_url") public val officialForumUrl: List<String?> = emptyList(),
    @SerialName("subreddit_url") public val subredditUrl: String? = null,
    @SerialName("twitter_screen_name") public val twitterScreenName: String? = null,
    @SerialName("repos_url") public val reposUrl: Map<String, List<String?>> = emptyMap(),
) {
    /** Block explorers without empty entries. */
    public val explorers: List<String> get() = blockchainSite.filterNotNull().filter { it.isNotBlank() }
}

/** `image` of a coin: the logo in three sizes on CoinGecko's image CDN. */
@Serializable
public data class CoinImage(
    public val thumb: String = "",
    public val small: String = "",
    public val large: String = "",
)

/**
 * `market_data` of a coin. Values per currency are keyed by lowercase vs currency (`usd`, `eur`, `btc`, ...); use the
 * functions that take a currency for single values.
 */
@Serializable
public data class CoinMarketData(
    @SerialName("current_price") public val currentPrice: Map<String, Double?> = emptyMap(),
    public val ath: Map<String, Double?> = emptyMap(),
    @SerialName("ath_change_percentage") public val athChangePercentage: Map<String, Double?> = emptyMap(),
    @SerialName("ath_date") public val athDate: Map<String, String?> = emptyMap(),
    public val atl: Map<String, Double?> = emptyMap(),
    @SerialName("atl_change_percentage") public val atlChangePercentage: Map<String, Double?> = emptyMap(),
    @SerialName("atl_date") public val atlDate: Map<String, String?> = emptyMap(),
    @SerialName("market_cap") public val marketCap: Map<String, Double?> = emptyMap(),
    @SerialName("market_cap_rank") public val marketCapRank: Int? = null,
    @SerialName("fully_diluted_valuation") public val fullyDilutedValuation: Map<String, Double?> = emptyMap(),
    @SerialName("total_volume") public val totalVolume: Map<String, Double?> = emptyMap(),
    @SerialName("high_24h") public val high24h: Map<String, Double?> = emptyMap(),
    @SerialName("low_24h") public val low24h: Map<String, Double?> = emptyMap(),
    /** The 24 hour change in USD. */
    @SerialName("price_change_24h") public val priceChange24h: Double? = null,
    @SerialName("price_change_percentage_24h") public val priceChangePercentage24h: Double? = null,
    @SerialName("price_change_percentage_7d") public val priceChangePercentage7d: Double? = null,
    @SerialName("price_change_percentage_14d") public val priceChangePercentage14d: Double? = null,
    @SerialName("price_change_percentage_30d") public val priceChangePercentage30d: Double? = null,
    @SerialName("price_change_percentage_60d") public val priceChangePercentage60d: Double? = null,
    @SerialName("price_change_percentage_200d") public val priceChangePercentage200d: Double? = null,
    @SerialName("price_change_percentage_1y") public val priceChangePercentage1y: Double? = null,
    @SerialName("market_cap_change_24h") public val marketCapChange24h: Double? = null,
    @SerialName("market_cap_change_percentage_24h") public val marketCapChangePercentage24h: Double? = null,
    @SerialName("price_change_percentage_1h_in_currency") public val change1hInCurrency: Map<String, Double?> = emptyMap(),
    @SerialName("price_change_percentage_24h_in_currency") public val change24hInCurrency: Map<String, Double?> = emptyMap(),
    @SerialName("price_change_percentage_7d_in_currency") public val change7dInCurrency: Map<String, Double?> = emptyMap(),
    @SerialName("price_change_percentage_30d_in_currency") public val change30dInCurrency: Map<String, Double?> = emptyMap(),
    @SerialName("price_change_percentage_1y_in_currency") public val change1yInCurrency: Map<String, Double?> = emptyMap(),
    @SerialName("total_supply") public val totalSupply: Double? = null,
    @SerialName("max_supply") public val maxSupply: Double? = null,
    @SerialName("circulating_supply") public val circulatingSupply: Double? = null,
    @SerialName("last_updated") public val lastUpdated: String? = null,
) {
    public fun price(currency: String): Double? = currentPrice[currency.lowercase()]
    public fun marketCap(currency: String): Double? = marketCap[currency.lowercase()]
    public fun volume(currency: String): Double? = totalVolume[currency.lowercase()]
    public fun high24h(currency: String): Double? = high24h[currency.lowercase()]
    public fun low24h(currency: String): Double? = low24h[currency.lowercase()]
    public fun ath(currency: String): Double? = ath[currency.lowercase()]
    public fun athChangePercentage(currency: String): Double? = athChangePercentage[currency.lowercase()]

    /** When the all-time high in [currency] was reached. */
    public fun athInstant(currency: String): Instant? = athDate[currency.lowercase()].toInstantOrNull()
    public fun atl(currency: String): Double? = atl[currency.lowercase()]
    public fun fullyDilutedValuation(currency: String): Double? = fullyDilutedValuation[currency.lowercase()]

    /** The 1 hour change in percent in [currency]. */
    public fun change1h(currency: String): Double? = change1hInCurrency[currency.lowercase()]

    /** The 24 hour change in percent in [currency], falling back to the USD based [priceChangePercentage24h]. */
    public fun change24h(currency: String): Double? =
        change24hInCurrency[currency.lowercase()] ?: priceChangePercentage24h.takeIf { currency.equals("usd", true) }

    public fun change7d(currency: String): Double? = change7dInCurrency[currency.lowercase()]
    public fun change30d(currency: String): Double? = change30dInCurrency[currency.lowercase()]
    public fun change1y(currency: String): Double? = change1yInCurrency[currency.lowercase()]
}

private val tag = Regex("<[^>]*>")
private val spaces = Regex("[ \\t\\x0B\\f]+")
private val blankLines = Regex("\\n{3,}")

/** Turns the API's description HTML into plain text. */
internal fun htmlToText(html: String): String = html
    .replace("\r\n", "\n")
    .replace(Regex("(?i)<br\\s*/?>"), "\n")
    .replace(Regex("(?i)</p\\s*>"), "\n\n")
    .replace(tag, "")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&nbsp;", " ")
    .replace(spaces, " ")
    .lines().joinToString("\n") { it.trim() }
    .replace(blankLines, "\n\n")
    .trim()
