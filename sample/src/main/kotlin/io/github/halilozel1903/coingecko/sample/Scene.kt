package io.github.halilozel1903.coingecko.sample

/**
 * Screenshot scenes, picked with the `scene` intent extra. They load live data from CoinGecko like a normal launch,
 * starting on a fixed [tab] and, for [Detail], on Bitcoin's detail screen. [loads] is how many loads must finish
 * before the scene is complete; the keyless API is rate limited, so every scene makes as few calls as it can.
 */
enum class Scene(val key: String, val tab: Tab, val coinId: String?, val loads: Int) {
    /** `/global` for the header and one page of `/coins/markets`. */
    Markets("markets", Tab.Markets, null, loads = 2),

    /** `/coins/bitcoin` and its 7 day `/market_chart`. */
    Detail("detail", Tab.Markets, "bitcoin", loads = 2),

    /** `/search/trending`. */
    Trending("trending", Tab.Trending, null, loads = 1),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}

/** The two tabs of the tracker. */
enum class Tab(val label: String) {
    Markets("Markets"),
    Trending("Trending"),
}
