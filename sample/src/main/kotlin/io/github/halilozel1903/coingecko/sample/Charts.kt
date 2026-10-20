package io.github.halilozel1903.coingecko.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.coingecko.model.ChartPoint

/** Green for gains, red for losses, in the current theme. */
@Composable
fun changeColor(value: Double?): Color = when {
    value == null -> MaterialTheme.colorScheme.onSurfaceVariant
    value >= 0 -> LocalTrackerColors.gain
    else -> LocalTrackerColors.loss
}

/** A small line of [values] without axes, colored by whether the last value is above the first. */
@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier) {
    if (values.size < 2) return
    val color = changeColor(values.last() - values.first())
    Canvas(modifier) {
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - ((v - min) / span * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * The price chart of the detail screen: a line with a gradient fill, dashed guides at the high and the low, price
 * labels on the right and time labels below. Touch and drag to read a point; [onSelect] gets it, or `null` on
 * release.
 */
@Composable
fun PriceChart(
    points: List<ChartPoint>,
    range: ChartRange,
    currency: Currency,
    modifier: Modifier = Modifier,
    onSelect: (ChartPoint?) -> Unit = {},
) {
    if (points.size < 2) return
    val values = points.map { it.value }
    val min = values.min()
    val max = values.max()
    val color = changeColor(values.last() - values.first())
    val guide = MaterialTheme.colorScheme.outlineVariant
    val marker = MaterialTheme.colorScheme.onSurface
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(220.dp)) {
            Canvas(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(points) {
                        fun pick(x: Float) {
                            val index = (x / size.width * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                            selected = index
                            onSelect(points[index])
                        }
                        detectHorizontalDragGestures(
                            onDragStart = { pick(it.x) },
                            onDragEnd = {
                                selected = null
                                onSelect(null)
                            },
                            onDragCancel = {
                                selected = null
                                onSelect(null)
                            },
                        ) { change, _ -> pick(change.position.x) }
                    }
                    .pointerInput(points) {
                        detectTapGestures(
                            onPress = { offset ->
                                val index = (offset.x / size.width * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                                selected = index
                                onSelect(points[index])
                                tryAwaitRelease()
                                selected = null
                                onSelect(null)
                            },
                        )
                    },
            ) {
                val top = 8.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                val span = (max - min).takeIf { it > 0 } ?: 1.0
                val step = size.width / (points.size - 1)
                fun y(v: Double): Float = bottom - ((v - min) / span * (bottom - top)).toFloat()

                val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
                drawLine(guide, Offset(0f, y(max)), Offset(size.width, y(max)), strokeWidth = 1.dp.toPx(), pathEffect = dash)
                drawLine(guide, Offset(0f, y(min)), Offset(size.width, y(min)), strokeWidth = 1.dp.toPx(), pathEffect = dash)

                val line = Path()
                values.forEachIndexed { i, v -> if (i == 0) line.moveTo(0f, y(v)) else line.lineTo(i * step, y(v)) }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f))))
                drawPath(line, color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                val highIndex = values.indexOf(max)
                val lowIndex = values.indexOf(min)
                drawCircle(color, radius = 3.5.dp.toPx(), center = Offset(highIndex * step, y(max)))
                drawCircle(color, radius = 3.5.dp.toPx(), center = Offset(lowIndex * step, y(min)))
                drawCircle(color, radius = 4.5.dp.toPx(), center = Offset(size.width, y(values.last())))

                selected?.let { i ->
                    val x = i * step
                    drawLine(marker.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                    drawCircle(marker, radius = 5.dp.toPx(), center = Offset(x, y(values[i])))
                }
            }
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                AxisLabel(Format.price(max, currency))
                AxisLabel(Format.price((max + min) / 2, currency))
                AxisLabel(Format.price(min, currency))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val first = points.first().timeMillis
            val last = points.last().timeMillis
            for (i in 0..4) {
                AxisLabel(Format.axis(first + (last - first) * i / 4, range))
            }
        }
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
