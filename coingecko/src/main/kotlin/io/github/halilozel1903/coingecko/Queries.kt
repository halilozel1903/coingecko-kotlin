package io.github.halilozel1903.coingecko

/** `order` of `/coins/markets`. */
public enum class MarketOrder(public val apiValue: String) {
    MarketCapDesc("market_cap_desc"),
    MarketCapAsc("market_cap_asc"),
    VolumeDesc("volume_desc"),
    VolumeAsc("volume_asc"),
    IdAsc("id_asc"),
    IdDesc("id_desc"),
}

/** A window of `price_change_percentage` in `/coins/markets`. */
public enum class PriceChangeWindow(public val apiValue: String) {
    Hour("1h"),
    Day("24h"),
    Week("7d"),
    TwoWeeks("14d"),
    Month("30d"),
    TwoHundredDays("200d"),
    Year("1y"),
}

/**
 * The parameters of `GET /coins/markets`. Build one with [marketsQuery] or pass the builder block to the client:
 *
 * ```kotlin
 * client.markets {
 *     vsCurrency = "eur"
 *     perPage = 50
 *     sparkline = true
 *     priceChange(PriceChangeWindow.Hour, PriceChangeWindow.Week)
 * }
 * ```
 */
public data class MarketsQuery(
    /** `vs_currency`: the currency of every price, such as `usd`, `eur` or `btc`. */
    public val vsCurrency: String = "usd",
    /** `ids`: only these coin ids. Empty for all coins. */
    public val ids: List<String> = emptyList(),
    /** `category`: only coins of this category id, such as `layer-1`. */
    public val category: String? = null,
    /** `order`: the sort order. */
    public val order: MarketOrder = MarketOrder.MarketCapDesc,
    /** `per_page`: coins per page, 1 to 250. */
    public val perPage: Int = 100,
    /** `page`: the page, from 1. */
    public val page: Int = 1,
    /** `sparkline`: include 7 days of hourly prices in [io.github.halilozel1903.coingecko.model.MarketCoin.sparkline7d]. */
    public val sparkline: Boolean = false,
    /** `price_change_percentage`: extra change windows, such as 1h and 7d. */
    public val priceChangePercentage: List<PriceChangeWindow> = emptyList(),
    /** `precision`: decimal places of prices, `"0"` to `"18"` or `"full"`. `null` uses the API default. */
    public val precision: String? = null,
) {
    init {
        require(vsCurrency.isNotBlank()) { "vsCurrency must not be blank" }
        require(perPage in 1..MAX_PER_PAGE) { "perPage must be between 1 and $MAX_PER_PAGE, was $perPage" }
        require(page >= 1) { "pages start at 1, was $page" }
        requireValidPrecision(precision)
    }

    /** The query parameters in a fixed order, unencoded. */
    public fun parameters(): List<Pair<String, String>> = buildList<Pair<String, String>> {
        add("vs_currency" to vsCurrency.trim().lowercase())
        if (ids.isNotEmpty()) add("ids" to ids.joinToString(",") { it.trim() })
        category?.takeIf { it.isNotBlank() }?.let { add("category" to it.trim()) }
        add("order" to order.apiValue)
        add("per_page" to perPage.toString())
        add("page" to page.toString())
        add("sparkline" to sparkline.toString())
        if (priceChangePercentage.isNotEmpty()) {
            add("price_change_percentage" to priceChangePercentage.distinct().joinToString(",") { it.apiValue })
        }
        precision?.let { add("precision" to it) }
    }

    /** A mutable builder for [MarketsQuery]; see [marketsQuery]. */
    public class Builder {
        public var vsCurrency: String = "usd"
        public var ids: List<String> = emptyList()
        public var category: String? = null
        public var order: MarketOrder = MarketOrder.MarketCapDesc
        public var perPage: Int = 100
        public var page: Int = 1
        public var sparkline: Boolean = false
        public var priceChangePercentage: List<PriceChangeWindow> = emptyList()
        public var precision: String? = null

        /** Shorthand for [ids]. */
        public fun ids(vararg ids: String) {
            this.ids = ids.toList()
        }

        /** Shorthand for [priceChangePercentage]. */
        public fun priceChange(vararg windows: PriceChangeWindow) {
            priceChangePercentage = windows.toList()
        }

        public fun build(): MarketsQuery = MarketsQuery(
            vsCurrency = vsCurrency,
            ids = ids,
            category = category,
            order = order,
            perPage = perPage,
            page = page,
            sparkline = sparkline,
            priceChangePercentage = priceChangePercentage,
            precision = precision,
        )
    }

    public companion object {
        /** The largest page `/coins/markets` serves. */
        public const val MAX_PER_PAGE: Int = 250
    }
}

/** Builds a [MarketsQuery]. */
public fun marketsQuery(block: MarketsQuery.Builder.() -> Unit): MarketsQuery = MarketsQuery.Builder().apply(block).build()

/**
 * The flags of `GET /coins/{id}`. The defaults trim the response to what an app usually shows: market data and the
 * English description, without localized names, tickers, community and developer data or the sparkline. Each of
 * those can make the response many times larger.
 */
public data class CoinOptions(
    /** `localization`: names and descriptions in every language. */
    public val localization: Boolean = false,
    /** `tickers`: the coin's trading pairs on exchanges. */
    public val tickers: Boolean = false,
    /** `market_data`: prices, market caps, volumes, supply and changes in every currency. */
    public val marketData: Boolean = true,
    /** `community_data`: social statistics. */
    public val communityData: Boolean = false,
    /** `developer_data`: repository statistics. */
    public val developerData: Boolean = false,
    /** `sparkline`: 7 days of prices in the market data. */
    public val sparkline: Boolean = false,
) {
    /** The query parameters in the API's order. */
    public fun parameters(): List<Pair<String, String>> = listOf(
        "localization" to localization.toString(),
        "tickers" to tickers.toString(),
        "market_data" to marketData.toString(),
        "community_data" to communityData.toString(),
        "developer_data" to developerData.toString(),
        "sparkline" to sparkline.toString(),
    )

    public companion object {
        /** Everything the endpoint can return. */
        public val Full: CoinOptions = CoinOptions(
            localization = true,
            tickers = true,
            marketData = true,
            communityData = true,
            developerData = true,
            sparkline = true,
        )
    }
}

/**
 * `days` of `/coins/{id}/market_chart`: how far back the chart goes. The API picks the granularity from it: about
 * 5 minutes for 1 day, hourly up to 90 days, daily beyond.
 */
public class ChartDays private constructor(public val apiValue: String) {
    override fun equals(other: Any?): Boolean = other is ChartDays && other.apiValue == apiValue
    override fun hashCode(): Int = apiValue.hashCode()
    override fun toString(): String = "ChartDays($apiValue)"

    public companion object {
        /** The last [days] days. */
        public fun of(days: Int): ChartDays {
            require(days >= 1) { "days must be at least 1, was $days" }
            return ChartDays(days.toString())
        }

        public val Day: ChartDays = of(1)
        public val Week: ChartDays = of(7)
        public val Month: ChartDays = of(30)
        public val Quarter: ChartDays = of(90)
        public val Year: ChartDays = of(365)

        /** All data since the coin was listed. Paid plans only on the current public API. */
        public val Max: ChartDays = ChartDays("max")
    }
}

/** `interval` of `/coins/{id}/market_chart`. Leave it `null` for the automatic granularity. */
public enum class ChartInterval(public val apiValue: String) {
    /** One point per day, at 00:00 UTC. */
    Daily("daily"),
}

internal fun requireValidPrecision(precision: String?) {
    if (precision == null) return
    require(precision == "full" || precision.toIntOrNull() in 0..18) {
        "precision must be \"full\" or 0 to 18, was \"$precision\""
    }
}
