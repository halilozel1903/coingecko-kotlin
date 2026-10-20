package io.github.halilozel1903.coingecko.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /search/trending`: the coins, NFTs and categories searched most on CoinGecko in the last 24 hours. */
@Serializable
public data class Trending(
    /** The trending coins as the API wraps them; see [coins]. */
    @SerialName("coins") public val items: List<TrendingCoinItem> = emptyList(),
    public val nfts: List<TrendingNft> = emptyList(),
    public val categories: List<TrendingCategory> = emptyList(),
) {
    /** The trending coins, most searched first. */
    public val coins: List<TrendingCoin> get() = items.map { it.item }
}

/** `{"item": {...}}`, the wrapper of each trending coin. */
@Serializable
public data class TrendingCoinItem(public val item: TrendingCoin)

@Serializable
public data class TrendingCoin(
    public val id: String,
    @SerialName("coin_id") public val coinId: Long? = null,
    public val name: String = "",
    public val symbol: String = "",
    @SerialName("market_cap_rank") public val marketCapRank: Int? = null,
    public val thumb: String = "",
    public val small: String = "",
    public val large: String = "",
    public val slug: String? = null,
    @SerialName("price_btc") public val priceBtc: Double? = null,
    /** The position in the list, from 0. */
    public val score: Int = 0,
    public val data: TrendingCoinData? = null,
) {
    /** The price in USD. */
    public val priceUsd: Double? get() = data?.price

    /** The 24 hour change in percent in USD. */
    public val change24hUsd: Double? get() = data?.priceChangePercentage24h?.get("usd")
}

/** Display data of a trending coin. Prices are in USD; market cap and volume come preformatted, such as `"$1,234"`. */
@Serializable
public data class TrendingCoinData(
    @Serializable(with = LenientDoubleSerializer::class) public val price: Double? = null,
    @SerialName("price_btc") public val priceBtc: String? = null,
    @SerialName("price_change_percentage_24h") public val priceChangePercentage24h: Map<String, Double?> = emptyMap(),
    @SerialName("market_cap") public val marketCap: String? = null,
    @SerialName("market_cap_btc") public val marketCapBtc: String? = null,
    @SerialName("total_volume") public val totalVolume: String? = null,
    @SerialName("total_volume_btc") public val totalVolumeBtc: String? = null,
    /** An SVG image of the 7 day price. */
    public val sparkline: String? = null,
) {
    /** [marketCap] as a number. */
    public val marketCapValue: Double? get() = parseFormattedNumber(marketCap)

    /** [totalVolume] as a number. */
    public val totalVolumeValue: Double? get() = parseFormattedNumber(totalVolume)
}

@Serializable
public data class TrendingNft(
    public val id: String,
    public val name: String = "",
    public val symbol: String? = null,
    public val thumb: String = "",
    @SerialName("nft_contract_id") public val nftContractId: Long? = null,
    @SerialName("native_currency_symbol") public val nativeCurrencySymbol: String? = null,
    @SerialName("floor_price_in_native_currency") public val floorPriceInNativeCurrency: Double? = null,
    @SerialName("floor_price_24h_percentage_change") public val floorPrice24hPercentageChange: Double? = null,
)

@Serializable
public data class TrendingCategory(
    @Serializable(with = LenientStringSerializer::class) public val id: String,
    public val name: String = "",
    @SerialName("market_cap_1h_change") public val marketCap1hChange: Double? = null,
    public val slug: String? = null,
    @SerialName("coins_count") @Serializable(with = LenientStringSerializer::class) public val coinsCount: String = "",
    public val data: TrendingCategoryData? = null,
)

@Serializable
public data class TrendingCategoryData(
    @SerialName("market_cap") public val marketCap: Double? = null,
    @SerialName("total_volume") public val totalVolume: Double? = null,
    @SerialName("market_cap_change_percentage_24h") public val marketCapChangePercentage24h: Map<String, Double?> = emptyMap(),
)
