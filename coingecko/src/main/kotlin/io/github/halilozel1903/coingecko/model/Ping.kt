package io.github.halilozel1903.coingecko.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /ping`: the API status. */
@Serializable
public data class Ping(
    /** `"(V3) To the Moon!"` when the API is up. */
    @SerialName("gecko_says") public val geckoSays: String = "",
)
