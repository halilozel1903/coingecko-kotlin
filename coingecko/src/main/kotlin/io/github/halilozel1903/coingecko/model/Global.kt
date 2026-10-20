package io.github.halilozel1903.coingecko.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
internal data class GlobalResponse(val data: GlobalData)

/** `GET /global`: the whole crypto market. */
@Serializable
public data class GlobalData(
    @SerialName("active_cryptocurrencies") public val activeCryptocurrencies: Int = 0,
    @SerialName("upcoming_icos") public val upcomingIcos: Int = 0,
    @SerialName("ongoing_icos") public val ongoingIcos: Int = 0,
    @SerialName("ended_icos") public val endedIcos: Int = 0,
    public val markets: Int = 0,
    /** The total market cap in every vs currency. */
    @SerialName("total_market_cap") public val totalMarketCap: Map<String, Double?> = emptyMap(),
    /** The total 24 hour volume in every vs currency. */
    @SerialName("total_volume") public val totalVolume: Map<String, Double?> = emptyMap(),
    /** Each large coin's share of the total market cap in percent, keyed by symbol (`btc`, `eth`, ...). */
    @SerialName("market_cap_percentage") public val marketCapPercentage: Map<String, Double?> = emptyMap(),
    @SerialName("market_cap_change_percentage_24h_usd") public val marketCapChangePercentage24hUsd: Double? = null,
    /** UNIX seconds. */
    @SerialName("updated_at") public val updatedAt: Long = 0,
) {
    public fun totalMarketCap(currency: String): Double? = totalMarketCap[currency.lowercase()]
    public fun totalVolume(currency: String): Double? = totalVolume[currency.lowercase()]

    /** Bitcoin's share of the total market cap, in percent. */
    public val btcDominance: Double? get() = marketCapPercentage["btc"]

    /** Ether's share of the total market cap, in percent. */
    public val ethDominance: Double? get() = marketCapPercentage["eth"]

    public val updatedInstant: Instant get() = Instant.ofEpochSecond(updatedAt)
}
