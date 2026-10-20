package io.github.halilozel1903.coingecko.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.delay

/** How often a failed image is requested again before it counts as failed. */
private const val ImageRetries = 3

/**
 * A coin's logo, loaded with Coil from CoinGecko's image CDN (the url the API returns) and clipped to a circle. A
 * tinted circle with the coin's first letter stands in while it loads, and for coins without a logo. A failed load is
 * retried a few times. Screenshot scenes wait until every logo on screen has loaded, and fail when one does not.
 */
@Composable
fun CoinImage(url: String, name: String, modifier: Modifier = Modifier) {
    val monitor = LocalSceneMonitor.current
    var attempt by remember(url) { mutableIntStateOf(0) }
    var status by remember(url) { mutableStateOf(ImageStatus.Loading) }
    // Coins without a logo come as "missing_large.png", a bare file name; show the letter for those.
    val hasUrl = url.startsWith("https://") || url.startsWith("http://")
    if (monitor != null && hasUrl) {
        val id = remember { Any() }
        DisposableEffect(monitor, id) { onDispose { monitor.image(id, null) } }
        // snapshotFlow starts with the current value, so a load that finished before this effect ran still counts.
        // A failure that will be retried still counts as loading.
        LaunchedEffect(monitor, id, url) {
            snapshotFlow {
                if (status == ImageStatus.Failed && attempt < ImageRetries) ImageStatus.Loading else status
            }.collect { monitor.image(id, it) }
        }
    }
    if (hasUrl && status == ImageStatus.Failed && attempt < ImageRetries) {
        LaunchedEffect(url, attempt) {
            delay(1_000L shl attempt)
            status = ImageStatus.Loading
            attempt++
        }
    }
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasUrl || status != ImageStatus.Loaded) {
            Text(
                name.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            )
        }
        if (hasUrl) {
            key(url, attempt) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    onState = { state ->
                        status = when (state) {
                            is AsyncImagePainter.State.Success -> ImageStatus.Loaded
                            is AsyncImagePainter.State.Error -> ImageStatus.Failed
                            else -> ImageStatus.Loading
                        }
                    },
                )
            }
        }
    }
}
