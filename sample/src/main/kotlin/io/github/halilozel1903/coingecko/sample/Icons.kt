package io.github.halilozel1903.coingecko.sample

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Small drawn icons, so the sample needs no icon library. */
@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.1f
        drawCircle(color, radius = s * 0.3f, center = Offset(s * 0.42f, s * 0.42f), style = Stroke(width = stroke))
        drawLine(color, Offset(s * 0.64f, s * 0.64f), Offset(s * 0.88f, s * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.1f
        drawLine(color, Offset(s * 0.22f, s * 0.22f), Offset(s * 0.78f, s * 0.78f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.78f, s * 0.22f), Offset(s * 0.22f, s * 0.78f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun BackIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.1f
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.82f, s * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.24f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.76f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** A rising line, for the Markets tab. */
@Composable
fun ChartIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.09f
        val path = Path().apply {
            moveTo(s * 0.14f, s * 0.72f)
            lineTo(s * 0.38f, s * 0.46f)
            lineTo(s * 0.56f, s * 0.6f)
            lineTo(s * 0.86f, s * 0.26f)
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(s * 0.12f, s * 0.86f), Offset(s * 0.88f, s * 0.86f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** A flame, for the Trending tab. */
@Composable
fun FlameIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.08f)
            cubicTo(s * 0.62f, s * 0.3f, s * 0.82f, s * 0.42f, s * 0.8f, s * 0.64f)
            cubicTo(s * 0.78f, s * 0.84f, s * 0.64f, s * 0.92f, s * 0.5f, s * 0.92f)
            cubicTo(s * 0.34f, s * 0.92f, s * 0.2f, s * 0.82f, s * 0.2f, s * 0.62f)
            cubicTo(s * 0.2f, s * 0.48f, s * 0.3f, s * 0.38f, s * 0.36f, s * 0.32f)
            cubicTo(s * 0.38f, s * 0.42f, s * 0.42f, s * 0.48f, s * 0.47f, s * 0.5f)
            cubicTo(s * 0.46f, s * 0.36f, s * 0.46f, s * 0.2f, s * 0.5f, s * 0.08f)
            close()
        }
        drawPath(path, color, style = Stroke(width = s * 0.08f, join = StrokeJoin.Round))
    }
}
