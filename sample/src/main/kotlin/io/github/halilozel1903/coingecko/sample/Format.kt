package io.github.halilozel1903.coingecko.sample

import java.math.BigDecimal
import java.math.MathContext
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Number formatting for prices, market caps and changes. */
object Format {
    private val locale = Locale.US

    private fun symbol(currency: Currency): String = when (currency) {
        Currency.Usd -> "$"
        Currency.Eur -> "€"
        Currency.Gbp -> "£"
        Currency.Jpy -> "¥"
    }

    /** A price: grouped with 2 decimals from 1 up, 4 significant digits below 1 (`$0.00001234`). */
    fun price(value: Double?, currency: Currency): String {
        if (value == null) return "–"
        val sign = if (value < 0) "-" else ""
        val v = abs(value)
        val text = when {
            v == 0.0 -> "0.00"
            currency == Currency.Jpy && v >= 100 -> grouped(v, 0)
            v >= 1 -> grouped(v, 2)
            else -> BigDecimal(v).round(MathContext(4)).stripTrailingZeros().toPlainString()
        }
        return "$sign${symbol(currency)}$text"
    }

    /** A large amount in short form: `$2.13T`, `$48.2B`, `$912.0M`. */
    fun compact(value: Double?, currency: Currency? = null): String {
        if (value == null) return "–"
        val prefix = currency?.let { symbol(it) }.orEmpty()
        val v = abs(value)
        val sign = if (value < 0) "-" else ""
        val text = when {
            v >= 1e12 -> String.format(locale, "%.2fT", v / 1e12)
            v >= 1e9 -> String.format(locale, "%.2fB", v / 1e9)
            v >= 1e6 -> String.format(locale, "%.2fM", v / 1e6)
            v >= 1e3 -> String.format(locale, "%.2fK", v / 1e3)
            else -> String.format(locale, "%.2f", v)
        }
        return "$sign$prefix$text"
    }

    /** A supply such as `19.93M BTC`. */
    fun supply(value: Double?, symbol: String): String =
        if (value == null) "–" else "${compact(value)} ${symbol.uppercase(locale)}"

    /** A change such as `+1.84%` or `-0.73%`. */
    fun percent(value: Double?): String {
        if (value == null) return "–"
        val sign = if (value > 0) "+" else ""
        return sign + String.format(locale, "%.2f%%", value)
    }

    /** A share such as `58.4%`. */
    fun share(value: Double?): String = if (value == null) "–" else String.format(locale, "%.1f%%", value)

    fun count(value: Int): String = NumberFormat.getIntegerInstance(locale).format(value)

    private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    private val dayFormat = DateTimeFormatter.ofPattern("EEE", locale)
    private val hourFormat = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val monthFormat = DateTimeFormatter.ofPattern("d MMM", locale)

    fun date(instant: Instant?): String = instant?.atZone(ZoneId.systemDefault())?.format(dateFormat) ?: "–"

    /** An axis label for a chart of [range]. */
    fun axis(timeMillis: Long, range: ChartRange): String {
        val time = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault())
        return when (range) {
            ChartRange.Day -> time.format(hourFormat)
            ChartRange.Week -> time.format(dayFormat)
            ChartRange.Month, ChartRange.Year -> time.format(monthFormat)
        }
    }

    private fun grouped(value: Double, decimals: Int): String {
        val format = NumberFormat.getNumberInstance(locale)
        format.minimumFractionDigits = decimals
        format.maximumFractionDigits = decimals
        return format.format(value)
    }
}
