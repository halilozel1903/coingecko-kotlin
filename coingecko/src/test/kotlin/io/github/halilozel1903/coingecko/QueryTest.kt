package io.github.halilozel1903.coingecko

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QueryTest {

    private val client = CoinGeckoClient { transport = io.github.halilozel1903.coingecko.transport.CoinGeckoTransport { "{}" } }

    @Test
    fun buildsMarketsParametersInOrder() {
        val query = marketsQuery {
            vsCurrency = "EUR"
            ids("bitcoin", "ethereum")
            category = "layer-1"
            order = MarketOrder.VolumeDesc
            perPage = 50
            page = 2
            sparkline = true
            priceChange(PriceChangeWindow.Hour, PriceChangeWindow.Year, PriceChangeWindow.Hour)
            precision = "full"
        }
        assertEquals(
            "${CoinGeckoClient.DEFAULT_BASE_URL}coins/markets?vs_currency=eur&ids=bitcoin,ethereum&category=layer-1" +
                "&order=volume_desc&per_page=50&page=2&sparkline=true&price_change_percentage=1h,1y&precision=full",
            client.url("coins/markets", query.parameters()),
        )
    }

    @Test
    fun defaultsMarketsQuery() {
        assertEquals(
            listOf("vs_currency" to "usd", "order" to "market_cap_desc", "per_page" to "100", "page" to "1", "sparkline" to "false"),
            MarketsQuery().parameters(),
        )
    }

    @Test
    fun trimsCoinFlagsByDefault() {
        assertEquals(
            "localization=false&tickers=false&market_data=true&community_data=false&developer_data=false&sparkline=false",
            CoinOptions().parameters().joinToString("&") { "${it.first}=${it.second}" },
        )
        assertEquals(6, CoinOptions.Full.parameters().count { it.second == "true" })
    }

    @Test
    fun encodesValues() {
        assertEquals(
            "${CoinGeckoClient.DEFAULT_BASE_URL}search?query=shiba%20inu%26co",
            client.url("search", listOf("query" to "shiba inu&co")),
        )
    }

    @Test
    fun validatesQueries() {
        assertFailsWith<IllegalArgumentException> { MarketsQuery(perPage = 0) }
        assertFailsWith<IllegalArgumentException> { MarketsQuery(page = 0) }
        assertFailsWith<IllegalArgumentException> { MarketsQuery(vsCurrency = " ") }
        assertFailsWith<IllegalArgumentException> { MarketsQuery(precision = "19") }
        assertEquals("7", ChartDays.Week.apiValue)
        assertEquals("max", ChartDays.Max.apiValue)
        assertEquals(ChartDays.of(30), ChartDays.Month)
    }

    @Test
    fun usesACustomBaseUrl() {
        val proxied = CoinGeckoClient {
            baseUrl = "https://example.com/cg"
            transport = io.github.halilozel1903.coingecko.transport.CoinGeckoTransport { "{}" }
        }
        assertEquals("https://example.com/cg/global", proxied.url("global"))
    }
}
