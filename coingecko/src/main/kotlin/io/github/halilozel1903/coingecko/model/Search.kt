package io.github.halilozel1903.coingecko.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /search`: coins, exchanges, categories and NFTs matching a query, best matches first. */
@Serializable
public data class SearchResult(
    public val coins: List<SearchCoin> = emptyList(),
    public val exchanges: List<SearchExchange> = emptyList(),
    public val categories: List<SearchCategory> = emptyList(),
    public val nfts: List<SearchNft> = emptyList(),
)

@Serializable
public data class SearchCoin(
    public val id: String,
    public val name: String = "",
    @SerialName("api_symbol") public val apiSymbol: String? = null,
    /** The ticker in uppercase, such as `BTC`. */
    public val symbol: String = "",
    @SerialName("market_cap_rank") public val marketCapRank: Int? = null,
    /** The logo, small. */
    public val thumb: String = "",
    /** The logo, 250 px. */
    public val large: String = "",
)

@Serializable
public data class SearchExchange(
    public val id: String,
    public val name: String = "",
    @SerialName("market_type") public val marketType: String? = null,
    public val thumb: String = "",
    public val large: String = "",
)

@Serializable
public data class SearchCategory(
    /** The category id; older responses send a number, newer ones a slug. */
    @Serializable(with = LenientStringSerializer::class) public val id: String,
    public val name: String = "",
)

@Serializable
public data class SearchNft(
    public val id: String,
    public val name: String = "",
    public val symbol: String? = null,
    public val thumb: String = "",
)
