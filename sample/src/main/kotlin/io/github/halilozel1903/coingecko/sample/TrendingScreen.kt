package io.github.halilozel1903.coingecko.sample

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.coingecko.model.Trending
import io.github.halilozel1903.coingecko.model.TrendingCategory
import io.github.halilozel1903.coingecko.model.TrendingCoin

/** `/search/trending`: the coins searched most on CoinGecko in the last 24 hours, then the trending categories. */
@Composable
fun TrendingScreen(trending: Load<Trending>, onOpen: (String) -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    when (trending) {
        Load.Loading -> LoadingState(modifier)
        is Load.Failed -> ErrorState(trending.message, onRetry, modifier)
        is Load.Ready -> LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item(key = "title") { ScreenTitle("Trending coins", "Most searched on CoinGecko in the last 24 hours") }
            itemsIndexed(trending.value.coins, key = { _, coin -> coin.id }) { index, coin ->
                TrendingRow(index + 1, coin, onClick = { onOpen(coin.id) })
            }
            val categories = trending.value.categories
            if (categories.isNotEmpty()) {
                item(key = "categories") {
                    Text(
                        "Trending categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                    )
                }
                itemsIndexed(categories, key = { _, category -> "category-${category.id}" }) { _, category -> CategoryRow(category) }
            }
            item(key = "attribution") { Attribution() }
        }
    }
}

@Composable
private fun TrendingRow(position: Int, coin: TrendingCoin, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$position",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(26.dp),
            )
            CoinImage(coin.large.ifBlank { coin.small }, coin.name, Modifier.size(38.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(coin.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    coin.symbol.uppercase() + (coin.marketCapRank?.let { " · Rank #$it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.price(coin.priceUsd, Currency.Usd),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ChangeBadge(coin.change24hUsd)
            }
        }
    }
}

@Composable
private fun CategoryRow(category: TrendingCategory) {
    val change = category.data?.marketCapChangePercentage24h?.get("usd")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "Market cap " + Format.compact(category.data?.marketCap, Currency.Usd) +
                    (category.coinsCount.takeIf { it.isNotBlank() }?.let { " · $it coins" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box { ChangeBadge(change) }
    }
}
