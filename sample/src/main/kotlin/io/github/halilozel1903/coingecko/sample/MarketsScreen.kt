package io.github.halilozel1903.coingecko.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.coingecko.model.GlobalData
import io.github.halilozel1903.coingecko.model.MarketCoin
import io.github.halilozel1903.coingecko.model.SearchCoin
import io.github.halilozel1903.coingecko.model.SearchResult
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

const val COINGECKO_URL = "https://www.coingecko.com/en/api"

/** The markets list of one currency: the first page, then more pages of 50 as the list scrolls. */
class MarketsController(private val repository: CryptoRepository, val currency: Currency) {
    var first: Load<List<MarketCoin>> by mutableStateOf(Load.Loading)
        private set

    val coins = mutableStateListOf<MarketCoin>()

    var loadingMore: Boolean by mutableStateOf(false)
        private set

    var moreFailed: Boolean by mutableStateOf(false)
        private set

    private var page = 1
    private var hasMore = true
    private var started = false

    /** Loads the first page, once; returning from a coin does not load it again. */
    suspend fun ensureStarted() {
        if (started) return
        started = true
        start()
    }

    private suspend fun start() {
        first = Load.Loading
        val result = load { repository.markets(currency, 1) }
        if (result is Load.Ready) {
            coins.clear()
            coins.addAll(result.value)
            page = 1
            hasMore = result.value.size == CryptoRepository.PAGE_SIZE
        }
        first = result
    }

    suspend fun loadMore() {
        if (loadingMore || moreFailed || !hasMore || first !is Load.Ready) return
        loadingMore = true
        when (val result = load { repository.markets(currency, page + 1) }) {
            is Load.Ready -> {
                page++
                hasMore = result.value.size == CryptoRepository.PAGE_SIZE
                // Ranks move while paging; skip coins already shown.
                val shown = coins.mapTo(HashSet()) { it.id }
                coins.addAll(result.value.filter { it.id !in shown })
            }
            is Load.Failed -> moreFailed = true
            Load.Loading -> Unit
        }
        loadingMore = false
    }

    fun retryMore() {
        moreFailed = false
    }
}

@Composable
fun MarketsScreen(
    markets: MarketsController,
    global: Load<GlobalData>,
    currency: Currency,
    onCurrencyChange: (Currency) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    search: Load<SearchResult>?,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(markets, listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 6
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { markets.loadMore() }
    }

    LazyColumn(modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
        item(key = "title") { ScreenTitle("Markets") }
        item(key = "global") { GlobalHeader(global, currency, onRetry) }
        item(key = "search") {
            SearchField(query, onQueryChange)
        }
        if (search != null) {
            searchResults(search, onOpen)
        } else {
            item(key = "currencies") { CurrencyChips(currency, onCurrencyChange) }
            item(key = "columns") { ColumnHeader() }
            when (val first = markets.first) {
                Load.Loading -> item(key = "loading") { Box(Modifier.fillMaxWidth().height(240.dp)) { LoadingState() } }
                is Load.Failed -> item(key = "error") {
                    Box(Modifier.fillMaxWidth().height(360.dp)) { ErrorState(first.message, onRetry) }
                }
                is Load.Ready -> {
                    items(markets.coins, key = { it.id }) { coin ->
                        MarketRow(coin, currency, onClick = { onOpen(coin.id) })
                    }
                    item(key = "more") {
                        when {
                            markets.loadingMore -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                            markets.moreFailed -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = {
                                    markets.retryMore()
                                    scope.launch { markets.loadMore() }
                                }) { Text("Load more") }
                            }
                        }
                    }
                }
            }
        }
        item(key = "attribution") { Attribution() }
    }
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "Data provided by CoinGecko", linked, as CoinGecko's attribution guidelines ask. */
@Composable
fun Attribution(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Text(
        "Data provided by CoinGecko",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(COINGECKO_URL) }
            .padding(16.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun GlobalHeader(global: Load<GlobalData>, currency: Currency, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        when (global) {
            Load.Loading -> Box(Modifier.fillMaxWidth().height(116.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
            }
            is Load.Failed -> Column(Modifier.padding(16.dp)) {
                Text(global.message, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRetry) { Text("Try again") }
            }
            is Load.Ready -> {
                val data = global.value
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Text("Global market cap", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            Format.compact(data.totalMarketCap(currency.code), currency),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            Format.percent(data.marketCapChangePercentage24hUsd) + " 24h",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = changeColor(data.marketCapChangePercentage24hUsd),
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("BTC dominance", Format.share(data.btcDominance))
                        Stat("ETH dominance", Format.share(data.ethDominance))
                        Stat("24h volume", Format.compact(data.totalVolume(currency.code), currency))
                        Stat("Coins", Format.count(data.activeCryptocurrencies))
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search coins") },
        leadingIcon = { SearchIcon(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp)) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    CloseIcon(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(18.dp))
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun CurrencyChips(currency: Currency, onChange: (Currency) -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Currency.entries.forEach { option ->
            FilterChip(
                selected = option == currency,
                onClick = { onChange(option) },
                label = { Text(option.code.uppercase()) },
            )
        }
    }
}

@Composable
private fun ColumnHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    ) {
        val style = MaterialTheme.typography.labelSmall
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Text("#  Coin", style = style, color = color, modifier = Modifier.weight(1f))
        Text("7 days", style = style, color = color, modifier = Modifier.width(76.dp))
        Text("Price · 24h", style = style, color = color)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun MarketRow(coin: MarketCoin, currency: Currency, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            coin.marketCapRank?.toString() ?: "–",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(26.dp),
        )
        CoinImage(coin.image, coin.name, Modifier.size(34.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(coin.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${coin.symbol.uppercase()} · ${Format.compact(coin.marketCap, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Sparkline(
            coin.sparkline7d?.points.orEmpty(),
            Modifier
                .padding(horizontal = 8.dp)
                .width(60.dp)
                .height(28.dp),
        )
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(98.dp)) {
            Text(Format.price(coin.currentPrice, currency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            ChangeBadge(coin.priceChangePercentage24h)
        }
    }
}

/** A change in percent on a tinted pill. */
@Composable
fun ChangeBadge(value: Double?, modifier: Modifier = Modifier) {
    val color = changeColor(value)
    Text(
        Format.percent(value),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.searchResults(search: Load<SearchResult>, onOpen: (String) -> Unit) {
    when (search) {
        Load.Loading -> item(key = "search-loading") { Box(Modifier.fillMaxWidth().height(200.dp)) { LoadingState() } }
        is Load.Failed -> item(key = "search-error") {
            Text(search.message, modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
        }
        is Load.Ready -> {
            val coins = search.value.coins
            if (coins.isEmpty()) {
                item(key = "search-empty") {
                    Text("No coins found", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(coins.take(25), key = { "search-${it.id}" }) { coin -> SearchRow(coin, onClick = { onOpen(coin.id) }) }
        }
    }
}

@Composable
private fun SearchRow(coin: SearchCoin, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinImage(coin.large.ifBlank { coin.thumb }, coin.name, Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(coin.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(coin.symbol, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        coin.marketCapRank?.let {
            Text("#$it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
