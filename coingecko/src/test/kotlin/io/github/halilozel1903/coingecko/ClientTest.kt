package io.github.halilozel1903.coingecko

import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientTest {

    @Test
    fun pings() = runTest {
        MockApi().use { api ->
            assertEquals("(V3) To the Moon!", api.client.ping().geckoSays)
            assertEquals(listOf("${CoinGeckoClient.DEFAULT_BASE_URL}ping"), api.urls)
        }
    }

    @Test
    fun groupsSimplePriceKeys() = runTest {
        MockApi().use { api ->
            val prices = api.client.simplePrice(
                ids = listOf("bitcoin", "ethereum"),
                vsCurrencies = listOf("USD", "eur"),
                includeMarketCap = true,
                include24hVolume = true,
                include24hChange = true,
                includeLastUpdatedAt = true,
            )
            assertEquals(setOf("bitcoin", "ethereum"), prices.keys)
            val btc = prices.getValue("bitcoin")
            assertEquals(mapOf("usd" to 106912.0, "eur" to 91884.0), btc.prices)
            assertEquals(2129361508215.73, btc.marketCap("USD"))
            assertEquals(48211933618.41, btc.volume24h("usd"))
            assertEquals(1.8423, btc.change24h("usd"))
            assertEquals(1792228860L, btc.lastUpdatedAt)
            assertEquals(-0.9644, prices.getValue("ethereum").change24h("eur"))
        }
    }

    @Test
    fun loadsOnePrice() = runTest {
        MockApi().use { api ->
            assertEquals(106912.0, api.client.price("bitcoin"))
        }
    }

    @Test
    fun decodesMarkets() = runTest {
        MockApi().use { api ->
            val coins = api.client.markets {
                perPage = 3
                sparkline = true
                priceChange(PriceChangeWindow.Hour, PriceChangeWindow.Day, PriceChangeWindow.Week)
            }
            assertEquals(listOf("bitcoin", "ethereum", "tether"), coins.map { it.id })
            val btc = coins.first()
            assertEquals("btc", btc.symbol)
            assertEquals(1, btc.marketCapRank)
            assertEquals(106912.0, btc.currentPrice)
            assertEquals(21000000.0, btc.maxSupply)
            assertTrue(btc.image.startsWith("https://coin-images.coingecko.com/coins/images/1/large/"))
            assertEquals(0.2141, btc.priceChangePercentage(PriceChangeWindow.Hour))
            assertEquals(2.8817, btc.priceChangePercentage(PriceChangeWindow.Week))
            assertNull(btc.priceChangePercentage(PriceChangeWindow.Month))
            assertNull(btc.roi)
            assertEquals(Instant.parse("2025-10-06T18:57:42.558Z"), btc.athInstant)
            // One gap in the sparkline is dropped from points.
            assertEquals(168, btc.sparkline7d!!.price.size)
            assertEquals(167, btc.sparkline7d!!.points.size)
            val eth = coins[1]
            assertNull(eth.maxSupply)
            assertEquals("btc", eth.roi!!.currency)
        }
    }

    @Test
    fun loadsATrimmedCoin() = runTest {
        MockApi().use { api ->
            val coin = api.client.coin("bitcoin")
            assertEquals("Bitcoin", coin.name)
            assertEquals("SHA-256", coin.hashingAlgorithm)
            assertEquals("http://www.bitcoin.org", coin.homepage)
            assertEquals(listOf("https://mempool.space/", "https://blockchair.com/bitcoin/"), coin.links.explorers)
            assertEquals(listOf("https://github.com/bitcoin/bitcoin"), coin.links.reposUrl["github"])
            assertTrue(coin.image.large.endsWith("/large/bitcoin.png?1696501400"))
            assertEquals("2009-01-03", coin.genesisDate)
            assertTrue(coin.descriptionText.startsWith("Bitcoin is the first successful internet money"))
            assertTrue(coin.descriptionText.endsWith("digital currency & uses SHA-256 for proof of work."))
            assertFalse(coin.descriptionText.contains("<a"))

            val market = coin.marketData!!
            assertEquals(106912.0, market.price("usd"))
            assertEquals(91884.0, market.price("EUR"))
            assertEquals(126080.0, market.ath("usd"))
            assertEquals(Instant.parse("2025-10-06T18:57:42.558Z"), market.athInstant("usd"))
            assertEquals(19934262.0, market.circulatingSupply)
            assertEquals(1.8423, market.change24h("usd"))
            assertEquals(0.2141, market.change1h("usd"))
            assertEquals(58.2276, market.change1y("usd"))
            assertNull(market.change7d("gbp"))
        }
    }

    @Test
    fun loadsAMarketChart() = runTest {
        MockApi().use { api ->
            val chart = api.client.marketChart("bitcoin", "usd", 7)
            assertEquals(170, chart.prices.size)
            assertEquals(170, chart.marketCaps.size)
            assertEquals(170, chart.totalVolumes.size)
            assertEquals(1791624000000L, chart.prices.first().timeMillis)
            assertEquals(106912.0, chart.prices.last().value)
            assertTrue(chart.prices.zipWithNext().all { (a, b) -> a.timeMillis < b.timeMillis })
            assertTrue(chart.maxPrice!!.value >= chart.minPrice!!.value)
            assertTrue(chart.changePercentage!! > 0)
        }
    }

    @Test
    fun searches() = runTest {
        MockApi().use { api ->
            val result = api.client.search(" bitcoin ")
            assertEquals(listOf("bitcoin", "wrapped-bitcoin", "bitcoin-cash"), result.coins.map { it.id })
            assertEquals("BTC", result.coins.first().symbol)
            assertNull(result.coins[2].marketCapRank)
            // Category ids come as slugs or numbers.
            assertEquals(listOf("bitcoin-ecosystem", "104"), result.categories.map { it.id })
            assertEquals("spot", result.exchanges.single().marketType)
            assertEquals("PUPS", result.nfts.single().symbol)
        }
    }

    @Test
    fun decodesTrending() = runTest {
        MockApi().use { api ->
            val trending = api.client.trending()
            assertEquals(listOf("solana", "pepe", "sui"), trending.coins.map { it.id })
            val sol = trending.coins.first()
            assertEquals(0, sol.score)
            assertEquals(187.42, sol.priceUsd)
            assertEquals(4.2117, sol.change24hUsd)
            assertEquals(101823411220.0, sol.data!!.marketCapValue)
            assertTrue(sol.large.contains("/large/"))
            assertEquals(0.00000981, trending.coins[1].priceUsd)
            // An older response shape sends the price as formatted text.
            assertEquals(3.1102, trending.coins[2].priceUsd)
            assertEquals("Pudgy Penguins", trending.nfts.single().name)
            val category = trending.categories.single()
            assertEquals("251", category.id)
            assertEquals("812", category.coinsCount)
            assertEquals(5.12, category.data!!.marketCapChangePercentage24h["usd"])
        }
    }

    @Test
    fun decodesGlobal() = runTest {
        MockApi().use { api ->
            val global = api.client.global()
            assertEquals(19021, global.activeCryptocurrencies)
            assertEquals(1381, global.markets)
            assertEquals(3648021110233.4, global.totalMarketCap("USD"))
            assertEquals(58.37, global.btcDominance)
            assertEquals(12.82, global.ethDominance)
            assertEquals(1.4231, global.marketCapChangePercentage24hUsd)
            assertEquals(Instant.ofEpochSecond(1792228800), global.updatedInstant)
        }
    }

    @Test
    fun sendsNoKeyByDefault() = runTest {
        MockApi().use { api ->
            api.client.ping()
            val headers = api.requests.single().headers
            assertEquals(ApiPlan.Public, api.client.plan)
            assertNull(headers[CoinGeckoClient.DEMO_KEY_HEADER])
            assertNull(headers[CoinGeckoClient.PRO_KEY_HEADER])
            assertTrue(headers[HttpHeaders.UserAgent]!!.startsWith("coingecko-kotlin/"))
            assertEquals("application/json", headers[HttpHeaders.Accept])
        }
    }

    @Test
    fun sendsADemoKeyToThePublicRoot() = runTest {
        MockApi(configure = { demoApiKey = " CG-demo123 " }).use { api ->
            api.client.ping()
            val request = api.requests.single()
            assertEquals(ApiPlan.Demo, api.client.plan)
            assertEquals("CG-demo123", request.headers[CoinGeckoClient.DEMO_KEY_HEADER])
            assertEquals("${CoinGeckoClient.DEFAULT_BASE_URL}ping", request.url.toString())
        }
    }

    @Test
    fun switchesToTheProRootWithAProKey() = runTest {
        MockApi(configure = { proApiKey = "CG-pro456" }).use { api ->
            api.client.global()
            val request = api.requests.single()
            assertEquals(ApiPlan.Pro, api.client.plan)
            assertEquals("CG-pro456", request.headers[CoinGeckoClient.PRO_KEY_HEADER])
            assertNull(request.headers[CoinGeckoClient.DEMO_KEY_HEADER])
            assertEquals("${CoinGeckoClient.PRO_BASE_URL}global", request.url.toString())
        }
    }

    @Test
    fun rejectsTwoKeys() {
        assertFailsWith<IllegalArgumentException> {
            CoinGeckoClient {
                demoApiKey = "a"
                proApiKey = "b"
            }
        }
    }

    @Test
    fun mapsNotFound() = runTest {
        MockApi().use { api ->
            val error = assertFailsWith<CoinGeckoException.NotFound> { api.client.coin("no-such-coin") }
            assertTrue(error.message!!.contains("coin not found"))
        }
    }

    @Test
    fun wrapsTransportFailures() = runTest {
        val client = CoinGeckoClient { transport = io.github.halilozel1903.coingecko.transport.CoinGeckoTransport { throw java.io.IOException("offline") } }
        val error = assertFailsWith<CoinGeckoException.Network> { client.ping() }
        assertEquals("${CoinGeckoClient.DEFAULT_BASE_URL}ping", error.url)
    }

    @Test
    fun servesACustomTransport() = runTest {
        val client = CoinGeckoClient { transport = io.github.halilozel1903.coingecko.transport.CoinGeckoTransport { fixture("global.json") } }
        assertEquals(58.37, client.global().btcDominance)
    }

    @Test
    fun reportsDecodingErrors() = runTest {
        val client = CoinGeckoClient { transport = io.github.halilozel1903.coingecko.transport.CoinGeckoTransport { "<html>maintenance</html>" } }
        assertFailsWith<CoinGeckoException.Decoding> { client.global() }
        assertFailsWith<CoinGeckoException.Decoding> { client.simplePrice(listOf("bitcoin"), listOf("usd")) }
    }

    @Test
    fun validatesArgumentsBeforeSending() = runTest {
        MockApi().use { api ->
            assertFailsWith<IllegalArgumentException> { api.client.coin(" ") }
            assertFailsWith<IllegalArgumentException> { api.client.search("") }
            assertFailsWith<IllegalArgumentException> { api.client.simplePrice(emptyList(), listOf("usd")) }
            assertFailsWith<IllegalArgumentException> { api.client.markets { perPage = 251 } }
            assertFailsWith<IllegalArgumentException> { api.client.marketChart("bitcoin", "usd", 0) }
            assertTrue(api.requests.isEmpty())
        }
    }
}
