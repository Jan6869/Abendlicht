package de.abendlicht.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.abendlicht.app.Habit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class CardLook { Neutral, Focused, Receded, Excluded }

/** Entsättigt den Inhalt stufenlos – ausgeschlossene Karten verblassen zu einem sanften Grau. */
fun Modifier.saturation(value: () -> Float): Modifier = drawWithContent {
    val v = value()
    if (v >= 0.995f) {
        drawContent()
    } else {
        val paint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(v) })
        }
        drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), paint) }
        drawContent()
        drawIntoCanvas { it.restore() }
    }
}

private data class Particle(val angle: Float, val speed: Float, val size: Float, val color: Color, val spin: Float, val square: Boolean)

@Composable
fun HabitCard(
    habit: Habit,
    look: CardLook,
    done: Boolean,
    showToggle: Boolean,
    included: Boolean,
    time: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
) {
    val colors = remember(habit.scene) { sceneColors(habit.scene) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val baseScale = when (look) {
        CardLook.Focused -> 1.035f
        CardLook.Receded -> 0.965f
        CardLook.Excluded -> 0.975f
        CardLook.Neutral -> 1f
    }
    val scale by animateFloatAsState(baseScale * if (pressed) 0.975f else 1f, liftSpring(), label = "scale")
    val alpha by animateFloatAsState(
        when (look) { CardLook.Receded -> 0.42f; CardLook.Excluded -> 0.4f; else -> 1f }, calmSpring(), label = "alpha"
    )
    val lift by animateFloatAsState(if (look == CardLook.Focused) 1f else 0f, liftSpring(), label = "lift")
    val sat by animateFloatAsState(if (look == CardLook.Excluded) 0.1f else 1f, calmSpring(), label = "sat")

    // Kleines Feuerwerk, wenn die Karte frisch abgehakt wird
    val burst = remember { Animatable(1f) }
    var prevDone by remember { mutableStateOf(done) }
    LaunchedEffect(done) {
        if (done && !prevDone) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(1200, easing = LinearOutSlowInEasing))
        }
        prevDone = done
    }
    val particles = remember(habit.id) {
        val r = Random(habit.id.hashCode())
        val palette = listOf(colors.c, colors.a, Color.White, colors.b, colors.title.copy(alpha = 0.7f))
        List(18) {
            Particle(
                angle = r.nextFloat() * 6.283f,
                speed = 0.35f + r.nextFloat() * 0.65f,
                size = 3f + r.nextFloat() * 4f,
                color = palette[it % palette.size],
                spin = r.nextFloat() * 720f - 360f,
                square = r.nextBoolean(),
            )
        }
    }

    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                this.alpha = alpha
                translationY = -lift * 5.dp.toPx()
                shadowElevation = (1f + lift * 20f).dp.toPx()
                this.shape = shape
                clip = true
                ambientShadowColor = colors.title
                spotShadowColor = colors.title
            }
            .saturation { sat }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(listOf(colors.top, colors.bottom), Offset.Zero, Offset(size.width * 0.6f, size.height * 1.4f)))
            drawScene(habit.scene, colors, time())
        }

        Column(
            Modifier
                .padding(start = 24.dp, top = 20.dp, end = 12.dp)
                .fillMaxWidth(0.5f)
        ) {
            Text(
                habit.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 25.sp, lineHeight = 28.sp, fontWeight = FontWeight(600)),
                color = colors.title,
                maxLines = 3,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${habit.minutes} Min",
                style = MaterialTheme.typography.labelMedium,
                color = colors.title.copy(alpha = 0.85f),
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }

        // Erledigt-Abzeichen
        AnimatedVisibility(
            visible = done && !showToggle,
            enter = scaleIn(liftSpring(), initialScale = 0.3f) + fadeIn(),
            exit = scaleOut(calmSpring(), targetScale = 0.6f) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
        ) {
            Box(
                Modifier.size(34.dp).background(Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Heute erledigt", tint = colors.title, modifier = Modifier.size(20.dp))
            }
        }

        // Auswahl-Kreis im Zufallsmodus
        AnimatedVisibility(
            visible = showToggle,
            enter = scaleIn(liftSpring(), initialScale = 0.4f) + fadeIn(),
            exit = scaleOut(calmSpring(), targetScale = 0.4f) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
        ) {
            val fill by animateFloatAsState(if (included) 1f else 0f, liftSpring(), label = "fill")
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.25f + 0.65f * fill), CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.95f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check, contentDescription = if (included) "Im Los" else "Ausgeschlossen",
                    tint = colors.title,
                    modifier = Modifier.size(18.dp).graphicsLayer { scaleX = fill; scaleY = fill; this.alpha = fill },
                )
            }
        }

        // Konfetti
        Canvas(Modifier.fillMaxSize()) {
            val p = burst.value
            if (p >= 1f) return@Canvas
            val origin = Offset(size.width - 31.dp.toPx(), 31.dp.toPx())
            val reach = size.height * 0.75f
            val fade = 1f - p
            particles.forEach { pt ->
                val d = reach * pt.speed * p
                val pos = origin + Offset(cos(pt.angle) * d, sin(pt.angle) * d + p * p * size.height * 0.35f)
                val s = pt.size.dp.toPx()
                val col = pt.color.copy(alpha = pt.color.alpha * fade)
                if (pt.square) {
                    rotate(pt.spin * p, pivot = pos) {
                        drawRect(col, topLeft = pos - Offset(s / 2, s / 3), size = Size(s, s * 0.66f))
                    }
                } else {
                    drawCircle(col, s / 2, pos)
                }
            }
        }
    }
}
