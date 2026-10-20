package io.github.halilozel1903.coingecko.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.coingecko.model.ChartPoint
import io.github.halilozel1903.coingecko.model.CoinDetail
import io.github.halilozel1903.coingecko.model.MarketChart
import java.time.Instant

/**
 * One coin: logo, price and 24h change, a price chart (7 days to start with), market stats and the description.
 * Loads `/coins/{id}` with the trimmed flags and `/coins/{id}/market_chart` for the selected range.
 */
@Composable
fun DetailScreen(
    coinId: String,
    repository: CryptoRepository,
    currency: Currency,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reload by remember { mutableIntStateOf(0) }
    var range by rememberSaveable { mutableStateOf(ChartRange.Week) }
    val coin by produceState<Load<CoinDetail>>(Load.Loading, coinId, reload) {
        value = Load.Loading
        value = load { repository.coin(coinId) }
    }
    val chart by produceState<Load<MarketChart>>(Load.Loading, coinId, currency, range, reload) {
        value = Load.Loading
        value = load { repository.chart(coinId, currency, range) }
    }
    ReportLoad(coin)
    ReportLoad(chart)

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { BackIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) }
            Text(
                (coin as? Load.Ready)?.value?.name ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        when (val state = coin) {
            Load.Loading -> LoadingState()
            is Load.Failed -> ErrorState(state.message, onRetry = { reload++ })
            is Load.Ready -> CoinContent(
                coin = state.value,
                chart = chart,
                range = range,
                onRangeChange = { range = it },
                currency = currency,
                onRetryChart = { reload++ },
            )
        }
    }
}

@Composable
private fun CoinContent(
    coin: CoinDetail,
    chart: Load<MarketChart>,
    range: ChartRange,
    onRangeChange: (ChartRange) -> Unit,
    currency: Currency,
    onRetryChart: () -> Unit,
) {
    val market = coin.marketData
    var selected by remember(chart) { mutableStateOf<ChartPoint?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinImage(coin.image.large, coin.name, Modifier.size(48.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(coin.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    coin.symbol.uppercase() + (coin.marketCapRank?.let { "  ·  Rank #$it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        val shownPrice = selected?.value ?: market?.price(currency.code)
        Text(Format.price(shownPrice, currency), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val point = selected
            if (point != null) {
                Text(Format.date(point.instant) + " " + Format.axis(point.timeMillis, ChartRange.Day), style = MaterialTheme.typography.labelLarge)
            } else {
                ChangeBadge(market?.change24h(currency.code))
                Spacer(Modifier.width(8.dp))
                Text("24h", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                (chart as? Load.Ready)?.value?.changePercentage?.let { change ->
                    Spacer(Modifier.width(14.dp))
                    ChangeBadge(change)
                    Spacer(Modifier.width(8.dp))
                    Text(range.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
            when (chart) {
                Load.Loading -> LoadingState()
                is Load.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(chart.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetryChart) { Text("Try again") }
                }
                is Load.Ready -> PriceChart(
                    points = chart.value.prices,
                    range = range,
                    currency = currency,
                    onSelect = { selected = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ChartRange.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == range,
                    onClick = { onRangeChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, ChartRange.entries.size),
                    label = { Text(option.label) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        if (market != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val symbol = coin.symbol
                    StatRow("Market cap", Format.compact(market.marketCap(currency.code), currency))
                    StatRow("24h volume", Format.compact(market.volume(currency.code), currency))
                    StatRow(
                        "24h range",
                        Format.price(market.low24h(currency.code), currency) + " – " + Format.price(market.high24h(currency.code), currency),
                    )
                    StatRow("All-time high", Format.price(market.ath(currency.code), currency), detail = athDetail(market.athChangePercentage(currency.code), market.athInstant(currency.code)))
                    StatRow("Circulating supply", Format.supply(market.circulatingSupply, symbol))
                    StatRow("Max supply", if (market.maxSupply == null) "Unlimited" else Format.supply(market.maxSupply, symbol))
                    StatRow("Fully diluted value", Format.compact(market.fullyDilutedValuation(currency.code), currency), divider = false)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ChangeCell("1h", market.change1h(currency.code))
                ChangeCell("24h", market.change24h(currency.code))
                ChangeCell("7d", market.change7d(currency.code))
                ChangeCell("30d", market.change30d(currency.code))
                ChangeCell("1y", market.change1y(currency.code))
            }
        }
        val about = coin.descriptionText
        if (about.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Text("About ${coin.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                about,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
        }
        coin.homepage?.let { homepage ->
            val uriHandler = LocalUriHandler.current
            TextButton(onClick = { uriHandler.openUri(homepage) }) { Text("Website") }
        }
        Attribution()
    }
}

private fun athDetail(change: Double?, date: Instant?): String? =
    if (change == null && date == null) null else listOfNotNull(change?.let { Format.percent(it) }, date?.let { Format.date(it) }).joinToString(" · ")

@Composable
private fun StatRow(label: String, value: String, detail: String? = null, divider: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@Composable
private fun ChangeCell(label: String, value: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            Format.percent(value),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = changeColor(value),
        )
    }
}
