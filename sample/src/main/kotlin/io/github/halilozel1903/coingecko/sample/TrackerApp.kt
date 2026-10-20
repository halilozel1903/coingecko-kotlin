package io.github.halilozel1903.coingecko.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.coingecko.model.SearchResult
import io.github.halilozel1903.coingecko.model.Trending
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A value loaded once, the first time a screen needs it, and again on [reload]. */
class LazyLoad<T>(private val block: suspend () -> T) {
    var state: Load<T> by mutableStateOf(Load.Loading)
        private set

    private var started = false

    suspend fun ensureLoaded() {
        if (started) return
        started = true
        state = load { block() }
    }

    suspend fun reload() {
        started = true
        state = Load.Loading
        state = load { block() }
    }
}

/**
 * The whole tracker: Markets (global header, search and the market list) and Trending tabs, and a coin's detail on
 * top, all loaded from the live CoinGecko API. Data is loaded only when a screen is shown, so a screenshot [scene]
 * makes only the calls it needs, and reports to a [SceneMonitor] when its data and logos have loaded.
 */
@Composable
fun TrackerApp(scene: Scene?, repository: CryptoRepository) {
    val monitor = remember(scene) { scene?.let { SceneMonitor(it) } }
    monitor?.Report()

    var tab by rememberSaveable { mutableStateOf(scene?.tab ?: Tab.Markets) }
    var currency by rememberSaveable { mutableStateOf(Currency.Usd) }
    var selected by rememberSaveable { mutableStateOf(scene?.coinId) }
    var query by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var reloadMarkets by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val global = remember(repository) { LazyLoad { repository.global() } }
    val trending = remember(repository) { LazyLoad<Trending> { repository.trending() } }
    val markets = remember(repository, currency, reloadMarkets) { MarketsController(repository, currency) }
    val listState = remember(markets) { LazyListState() }

    LaunchedEffect(query) {
        delay(450)
        search = query.trim()
    }

    CompositionLocalProvider(LocalSceneMonitor provides monitor) {
        val coinId = selected
        if (coinId != null) {
            BackHandler { selected = null }
            DetailScreen(
                coinId = coinId,
                repository = repository,
                currency = currency,
                onBack = { selected = null },
                modifier = Modifier.safeDrawingPadding(),
            )
        } else {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .statusBarsPadding(),
            ) {
                when (tab) {
                    Tab.Markets -> {
                        LaunchedEffect(global) { global.ensureLoaded() }
                        LaunchedEffect(markets) { markets.ensureStarted() }
                        ReportLoad(global.state)
                        ReportLoad(markets.first)
                        val searchState: Load<SearchResult>? = if (search.isBlank()) {
                            null
                        } else {
                            produceState<Load<SearchResult>>(Load.Loading, search) {
                                value = load { repository.search(search) }
                            }.value
                        }
                        MarketsScreen(
                            markets = markets,
                            global = global.state,
                            currency = currency,
                            onCurrencyChange = { currency = it },
                            query = query,
                            onQueryChange = { query = it },
                            search = searchState,
                            onOpen = { selected = it },
                            onRetry = {
                                reloadMarkets++
                                scope.launch { global.reload() }
                            },
                            listState = listState,
                        )
                    }
                    Tab.Trending -> {
                        LaunchedEffect(trending) { trending.ensureLoaded() }
                        ReportLoad(trending.state)
                        TrendingScreen(
                            trending = trending.state,
                            onOpen = { selected = it },
                            onRetry = { scope.launch { trending.reload() } },
                        )
                    }
                }
            }
            NavigationBar {
                Tab.entries.forEach { option ->
                    NavigationBarItem(
                        selected = option == tab,
                        onClick = { tab = option },
                        icon = {
                            val color = if (option == tab) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            when (option) {
                                Tab.Markets -> ChartIcon(color, Modifier.size(24.dp))
                                Tab.Trending -> FlameIcon(color, Modifier.size(24.dp))
                            }
                        },
                        label = { Text(option.label) },
                    )
                }
            }
        }
        }
    }
}
