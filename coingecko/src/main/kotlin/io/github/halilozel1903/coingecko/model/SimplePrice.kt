package io.github.halilozel1903.coingecko.model

/**
 * One coin of `GET /simple/price`. The API answers with flat keys per coin (`usd`, `usd_market_cap`,
 * `usd_24h_vol`, `usd_24h_change`, `last_updated_at`); they are grouped here by what they are, keyed by the
 * lowercase vs currency.
 */
public data class SimplePrice(
    /** The coin id, such as `bitcoin`. */
    public val id: String,
    /** The price in each requested currency. */
    public val prices: Map<String, Double>,
    /** The market cap in each currency, with `include_market_cap=true`. */
    public val marketCaps: Map<String, Double> = emptyMap(),
    /** The 24 hour trading volume in each currency, with `include_24hr_vol=true`. */
    public val volumes24h: Map<String, Double> = emptyMap(),
    /** The 24 hour price change in percent in each currency, with `include_24hr_change=true`. */
    public val changes24h: Map<String, Double> = emptyMap(),
    /** When the price was last updated, in UNIX seconds, with `include_last_updated_at=true`. */
    public val lastUpdatedAt: Long? = null,
) {
    /** The price in [currency] (any case), or `null` when it was not requested or is not known. */
    public fun price(currency: String): Double? = prices[currency.lowercase()]

    /** The market cap in [currency]. */
    public fun marketCap(currency: String): Double? = marketCaps[currency.lowercase()]

    /** The 24 hour volume in [currency]. */
    public fun volume24h(currency: String): Double? = volumes24h[currency.lowercase()]

    /** The 24 hour change in percent in [currency]. */
    public fun change24h(currency: String): Double? = changes24h[currency.lowercase()]
}
