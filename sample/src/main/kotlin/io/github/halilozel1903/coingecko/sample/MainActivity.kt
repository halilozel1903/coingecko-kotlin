package io.github.halilozel1903.coingecko.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

/**
 * A crypto tracker built on coingecko-kotlin. It loads the live CoinGecko API (keyless public tier) and the coins'
 * logos from CoinGecko's image CDN with Coil. `scripts/screenshots.sh` starts it with `--es scene <scene>` to open a
 * fixed screen, loaded live like a normal launch:
 *
 * - `markets`: the global header and the market list in USD
 * - `detail`: Bitcoin with its 7 day price chart and stats
 * - `trending`: the most searched coins of the last 24 hours
 *
 * Nothing is bundled or faked: when CoinGecko refuses a request, the screen shows the error and a scene fails.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { imageClient }))
                }
                .build()
        }
        setContent {
            TrackerTheme(dark = isSystemInDarkTheme()) {
                // The Surface makes text default to onBackground, so it stays readable in dark mode.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    TrackerApp(
                        scene = scene,
                        repository = if (scene != null) Repositories.scenes() else Repositories.live(),
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"

        private val imageClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "coingecko-kotlin-sample/1.0.0 (+https://github.com/halilozel1903/coingecko-kotlin)")
                            .build(),
                    )
                }
                .build()
        }
    }
}
