package io.github.halilozel1903.coingecko.model

import io.github.halilozel1903.coingecko.PriceChangeWindow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/** One coin of `GET /coins/markets`, in the requested vs currency. Values the API does not know are `null`. */
@Serializable
public data class MarketCoin(
    public val id: String,
    /** The ticker in lowercase, such as `btc`. */
    public val symbol: String = "",
    public val name: String = "",
    /** The logo, 250 px, on CoinGecko's image CDN. */
    public val image: String = "",
    @SerialName("current_price") public val currentPrice: Double? = null,
    @SerialName("market_cap") public val marketCap: Double? = null,
    @SerialName("market_cap_rank") public val marketCapRank: Int? = null,
    @SerialName("fully_diluted_valuation") public val fullyDilutedValuation: Double? = null,
    @SerialName("total_volume") public val totalVolume: Double? = null,
    @SerialName("high_24h") public val high24h: Double? = null,
    @SerialName("low_24h") public val low24h: Double? = null,
    @SerialName("price_change_24h") public val priceChange24h: Double? = null,
    @SerialName("price_change_percentage_24h") public val priceChangePercentage24h: Double? = null,
    @SerialName("market_cap_change_24h") public val marketCapChange24h: Double? = null,
    @SerialName("market_cap_change_percentage_24h") public val marketCapChangePercentage24h: Double? = null,
    @SerialName("circulating_supply") public val circulatingSupply: Double? = null,
    @SerialName("total_supply") public val totalSupply: Double? = null,
    @SerialName("max_supply") public val maxSupply: Double? = null,
    /** The all-time high price. */
    public val ath: Double? = null,
    @SerialName("ath_change_percentage") public val athChangePercentage: Double? = null,
    /** When [ath] was reached, ISO 8601. */
    @SerialName("ath_date") public val athDate: String? = null,
    /** The all-time low price. */
    public val atl: Double? = null,
    @SerialName("atl_change_percentage") public val atlChangePercentage: Double? = null,
    @SerialName("atl_date") public val atlDate: String? = null,
    /** Return on investment since the ICO, for coins that had one. */
    public val roi: Roi? = null,
    @SerialName("last_updated") public val lastUpdated: String? = null,
    /** Hourly prices of the last 7 days, with `sparkline=true`. */
    @SerialName("sparkline_in_7d") public val sparkline7d: Sparkline? = null,
    @SerialName("price_change_percentage_1h_in_currency") public val change1h: Double? = null,
    @SerialName("price_change_percentage_24h_in_currency") public val change24h: Double? = null,
    @SerialName("price_change_percentage_7d_in_currency") public val change7d: Double? = null,
    @SerialName("price_change_percentage_14d_in_currency") public val change14d: Double? = null,
    @SerialName("price_change_percentage_30d_in_currency") public val change30d: Double? = null,
    @SerialName("price_change_percentage_200d_in_currency") public val change200d: Double? = null,
    @SerialName("price_change_percentage_1y_in_currency") public val change1y: Double? = null,
) {
    /**
     * The price change in percent over [window], as requested with `price_change_percentage`. For 24 hours it falls
     * back to [priceChangePercentage24h], which the API always sends.
     */
    public fun priceChangePercentage(window: PriceChangeWindow): Double? = when (window) {
        PriceChangeWindow.Hour -> change1h
        PriceChangeWindow.Day -> change24h ?: priceChangePercentage24h
        PriceChangeWindow.Week -> change7d
        PriceChangeWindow.TwoWeeks -> change14d
        PriceChangeWindow.Month -> change30d
        PriceChangeWindow.TwoHundredDays -> change200d
        PriceChangeWindow.Year -> change1y
    }

    /** [lastUpdated] as an [Instant]. */
    public val lastUpdatedInstant: Instant? get() = lastUpdated.toInstantOrNull()

    /** [athDate] as an [Instant]. */
    public val athInstant: Instant? get() = athDate.toInstantOrNull()
}

/** `sparkline_in_7d`: about 168 hourly prices, oldest first. */
@Serializable
public data class Sparkline(
    /** The prices as the API sends them; gaps are `null`. */
    @SerialName("price") public val price: List<Double?> = emptyList(),
) {
    /** The prices without gaps. */
    public val points: List<Double> get() = price.filterNotNull()
}

/** `roi`: the return since the ICO. */
@Serializable
public data class Roi(
    public val times: Double? = null,
    public val currency: String? = null,
    public val percentage: Double? = null,
)

internal fun String?.toInstantOrNull(): Instant? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
