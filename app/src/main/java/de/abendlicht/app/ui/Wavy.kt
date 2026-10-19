package de.abendlicht.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Gewellte Fortschrittslinie im Stil von Material 3 Expressive (wie die Zeitleiste
 * der Medienwiedergabe auf dem Pixel). Die Welle wandert, solange der Timer läuft,
 * und glättet sich mit einer Feder zur geraden Linie, wenn pausiert wird.
 */
@Composable
fun WavyProgress(
    progress: () -> Float,
    playing: Boolean,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    thickness: Dp = 6.dp,
    amplitude: Dp = 4.5.dp,
    wavelength: Dp = 34.dp,
) {
    val reduce = LocalReduceMotion.current
    val amp by animateFloatAsState(
        targetValue = if (playing && !reduce) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 180f),
        label = "amp",
    )
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "phase",
    )
    val path = remember { Path() }

    Canvas(modifier.fillMaxWidth().height(32.dp)) {
        val stroke = thickness.toPx()
        val cy = size.height / 2
        val a = amplitude.toPx() * amp
        val wl = wavelength.toPx()
        val start = stroke / 2
        val endMax = size.width - stroke / 2
        val p = progress().coerceIn(0f, 1f)
        val head = start + (endMax - start) * p
        val gap = 7.dp.toPx()
        val ph = if (reduce) 0f else phase

        // Restliche Strecke: ruhig und gerade
        if (head + gap < endMax) {
            drawLine(trackColor, Offset(head + gap, cy), Offset(endMax, cy), stroke, StrokeCap.Round)
        }

        // Zurückgelegte Strecke: wellig, mit sanft auslaufender Amplitude am Anfang
        if (head > start + 0.5f) {
            path.reset()
            var x = start
            val step = 2.5f
            while (true) {
                val ramp = min(1f, (x - start) / (wl * 0.5f))
                val y = cy + a * ramp * sin(((x / wl) - ph) * 2f * PI.toFloat())
                if (x == start) path.moveTo(x, y) else path.lineTo(x, y)
                if (x >= head) break
                x = min(x + step, head)
            }
            drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Daumen: kleiner aufrechter Balken wie bei Material 3 Expressive
        val thumbH = 22.dp.toPx()
        drawLine(color, Offset(head, cy - thumbH / 2), Offset(head, cy + thumbH / 2), 5.dp.toPx(), StrokeCap.Round)
    }
}

/** Kleine statische Schmuck-Welle (z. B. unter Überschriften). */
@Composable
fun Squiggle(color: Color, modifier: Modifier = Modifier, waves: Int = 3) {
    Canvas(modifier) {
        val p = Path()
        val cy = size.height / 2
        val a = size.height * 0.3f
        var x = 0f
        p.moveTo(0f, cy)
        while (x <= size.width) {
            p.lineTo(x, cy + a * sin(x / size.width * waves * 2f * PI.toFloat()))
            x += 1.5f
        }
        drawPath(p, color, style = Stroke(size.height * 0.28f, cap = StrokeCap.Round))
    }
}
