package io.github.halilozel1903.coingecko.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant

/** `GET /coins/{id}/market_chart`: prices, market caps and volumes over time, oldest first. */
@Serializable
public data class MarketChart(
    @Serializable(with = ChartPointsSerializer::class) public val prices: List<ChartPoint> = emptyList(),
    @SerialName("market_caps")
    @Serializable(with = ChartPointsSerializer::class)
    public val marketCaps: List<ChartPoint> = emptyList(),
    @SerialName("total_volumes")
    @Serializable(with = ChartPointsSerializer::class)
    public val totalVolumes: List<ChartPoint> = emptyList(),
) {
    /** The lowest price of the chart. */
    public val minPrice: ChartPoint? get() = prices.minByOrNull { it.value }

    /** The highest price of the chart. */
    public val maxPrice: ChartPoint? get() = prices.maxByOrNull { it.value }

    /** The change from the first to the last price, in percent. `null` with fewer than two points. */
    public val changePercentage: Double?
        get() {
            val first = prices.firstOrNull()?.value ?: return null
            val last = prices.lastOrNull()?.value ?: return null
            if (prices.size < 2 || first == 0.0) return null
            return (last - first) / first * 100
        }
}

/** One point of a chart: a UNIX time in milliseconds and a value in the vs currency. */
@Serializable
public data class ChartPoint(public val timeMillis: Long, public val value: Double) {
    public val instant: Instant get() = Instant.ofEpochMilli(timeMillis)
}

/** Decodes the API's `[[timestamp, value], ...]` pairs. Pairs with a `null` value are skipped. */
internal object ChartPointsSerializer : KSerializer<List<ChartPoint>> {
    override val descriptor: SerialDescriptor = listSerialDescriptor(ChartPoint.serializer().descriptor)

    override fun deserialize(decoder: Decoder): List<ChartPoint> {
        val json = decoder as? JsonDecoder ?: throw SerializationException("Charts can only be read from JSON")
        val array = json.decodeJsonElement() as? JsonArray ?: throw SerializationException("Expected an array of pairs")
        return array.mapNotNull { pair ->
            val values = pair as? JsonArray ?: throw SerializationException("Expected a [time, value] pair, got $pair")
            val time = (values.getOrNull(0) as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            val value = (values.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            ChartPoint(time.toLong(), value)
        }
    }

    override fun serialize(encoder: Encoder, value: List<ChartPoint>) {
        throw SerializationException("Encoding charts is not supported")
    }
}
