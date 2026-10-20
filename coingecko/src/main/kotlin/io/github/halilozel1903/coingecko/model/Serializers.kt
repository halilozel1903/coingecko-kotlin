package io.github.halilozel1903.coingecko.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * A number that the API sends either as a JSON number or as a formatted string such as `"$1,234.56"`, as some
 * fields of `/search/trending` do. Text that is not a number decodes to `null`.
 */
internal object LenientDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeDouble()
        val primitive = element as? JsonPrimitive ?: return null
        if (primitive is JsonNull) return null
        if (!primitive.isString) return primitive.doubleOrNull
        return parseFormattedNumber(primitive.content)
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
    }
}

/** A value that the API sends either as a string or as a number, kept as text (ids of categories, for example). */
internal object LenientStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeString()
        val primitive = element as? JsonPrimitive ?: return element.toString()
        return if (primitive is JsonNull) "" else primitive.content
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

/** `"$1,234.56"` → 1234.56, `"1.2e-7"` → 1.2E-7. `null` for anything else. */
internal fun parseFormattedNumber(text: String?): Double? {
    if (text.isNullOrBlank()) return null
    val cleaned = text.trim().removePrefix("$").replace(",", "").replace(" ", "")
    return cleaned.toDoubleOrNull()
}
