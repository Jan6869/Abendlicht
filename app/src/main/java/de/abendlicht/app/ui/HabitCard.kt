package de.abendlicht.app.ui

import androidx.compose.animation.AnimatedVisibility
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import de.abendlicht.app.MAX_MINUTES
import de.abendlicht.app.MIN_MINUTES
import kotlin.math.roundToInt
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
    /** Gesetzt, solange die Karte ausgewählt ist – dann zeigt sie ihre Knöpfe. */
    actions: CardActions? = null,
) {
    val colors = remember(habit.scene) { sceneColors(habit.scene) }
    val expanded = actions != null
    // Beim Zuklappen sollen die Knöpfe noch sichtbar bleiben, bis sie verschwunden sind.
    val lastActions = remember { arrayOf<CardActions?>(null) }
    if (actions != null) lastActions[0] = actions
    val ex by animateFloatAsState(if (expanded) 1f else 0f, calmSpring(), label = "expand")
    val ground = remember(colors) { androidx.compose.ui.graphics.lerp(colors.bottom, Color.White, 0.22f) }
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
        val headerH = height
        Canvas(Modifier.matchParentSize()) {
            val hh = headerH.toPx()
            drawRect(Brush.linearGradient(listOf(colors.top, colors.bottom), Offset.Zero, Offset(size.width * 0.6f, hh * 1.4f)))
            if (size.height > hh) drawRect(ground.copy(alpha = ex), topLeft = Offset(0f, hh - 1f), size = Size(size.width, size.height - hh + 1f))
        }
        Column {
          Box(Modifier.fillMaxWidth().height(height)) {
            Canvas(Modifier.fillMaxSize()) {
                drawScene(habit.scene, colors, time())
                // Sanfter Übergang in die Knopfleiste statt harter Kante
                if (ex > 0.01f) {
                    val top = size.height * 0.78f
                    drawRect(
                        Brush.verticalGradient(listOf(ground.copy(alpha = 0f), ground.copy(alpha = ex)), startY = top, endY = size.height),
                        topLeft = Offset(0f, top), size = Size(size.width, size.height - top),
                    )
                }
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
          }
          AnimatedVisibility(
              visible = expanded,
              enter = expandVertically(liftSpring(), expandFrom = Alignment.Top) + fadeIn(calmSpring()),
              exit = shrinkVertically(calmSpring(), shrinkTowards = Alignment.Top) + fadeOut(calmSpring()),
          ) {
              lastActions[0]?.let { CardButtons(habit, done, colors, it) }
          }
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
        Canvas(Modifier.matchParentSize()) {
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

/** Was eine ausgewählte Karte direkt in sich anbietet. */
class CardActions(
    val onStartTimer: (Int) -> Unit,
    val onDone: () -> Unit,
    val onUnmark: () -> Unit,
    val onMinutesChange: (Int) -> Unit,
)

private fun snapMinutes(raw: Float): Int =
    ((raw / 5f).roundToInt() * 5).coerceIn(MIN_MINUTES, MAX_MINUTES)

fun tickHaptic(view: android.view.View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK else HapticFeedbackConstants.CLOCK_TICK
    )
}

@Composable
private fun CardButtons(habit: Habit, done: Boolean, colors: SceneColors, actions: CardActions) {
    val view = LocalView.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 14.dp),
    ) {
        TimerSlider(
            minutes = habit.minutes,
            colors = colors,
            onChange = actions.onMinutesChange,
            onStart = actions.onStartTimer,
            modifier = Modifier.weight(1f),
        )
        if (!done) {
            ActionPill("Erledigt", Icons.Rounded.Check, colors.title, Color.White, Modifier.weight(1f)) {
                confirmHaptic(view); actions.onDone()
            }
        } else {
            ActionPill("Rückgängig", Icons.AutoMirrored.Rounded.Undo, Color.White.copy(alpha = 0.6f), colors.title, Modifier.weight(1f)) {
                actions.onUnmark()
            }
        }
    }
}

@Composable
private fun ActionPill(
    text: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, liftSpring(), label = "press")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .clickable(interaction, indication = null, onClick = onClick),
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

/**
 * Timer-Knopf, der zugleich ein Regler ist – wie die Helligkeit im Pixel-Schnellzugriff.
 * Langsam waagrecht wischen verschiebt die Füllung und damit die Dauer (in 5-Minuten-Schritten,
 * mit leichtem Ticken). Antippen startet den Timer mit der gezeigten Dauer.
 */
@Composable
private fun TimerSlider(
    minutes: Int,
    colors: SceneColors,
    onChange: (Int) -> Unit,
    onStart: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var dragging by remember { mutableStateOf(false) }
    var raw by remember { mutableFloatStateOf(minutes.toFloat()) }
    LaunchedEffect(minutes) { if (!dragging) raw = minutes.toFloat() }
    val snapped = snapMinutes(raw)
    var lastTick by remember { mutableIntStateOf(snapped) }
    LaunchedEffect(snapped) {
        if (dragging && snapped != lastTick) tickHaptic(view)
        lastTick = snapped
    }
    val range = (MAX_MINUTES - MIN_MINUTES).toFloat()
    val target = if (dragging) (raw - MIN_MINUTES) / range else (snapped - MIN_MINUTES) / range
    // Beim Ziehen folgt die Füllung dem Finger 1:1, beim Loslassen federt sie auf die Stufe.
    val fill by animateFloatAsState(target, if (dragging) snap<Float>() else liftSpring<Float>(), label = "fill")
    val grow by animateFloatAsState(if (dragging) 1f else 0f, liftSpring(), label = "grow")
    val onChangeNow by rememberUpdatedState(onChange)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed && !dragging) 0.95f else 1f, liftSpring(), label = "press")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { val s = press * (1f + 0.04f * grow); scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.42f))
            .drawBehind {
                val h = size.height
                val w = h + (size.width - h) * fill.coerceIn(0f, 1f)
                drawRoundRect(Color.White.copy(alpha = 0.92f), size = Size(w, h), cornerRadius = CornerRadius(22.dp.toPx()))
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false; onChangeNow(snapMinutes(raw)) },
                    onDragCancel = { dragging = false; onChangeNow(snapMinutes(raw)) },
                ) { change, dx ->
                    change.consume()
                    raw = (raw + dx / size.width * range).coerceIn(MIN_MINUTES.toFloat(), MAX_MINUTES.toFloat())
                }
            }
            .clickable(interaction, indication = null) { onStart(snapped) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, null, tint = colors.title, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "$snapped Min",
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = colors.title, maxLines = 1,
            )
        }
        // Dezente Pfeile verraten, dass man wischen kann
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).graphicsLayer { alpha = 0.4f * (1f - grow) }) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = colors.title, modifier = Modifier.size(18.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.title, modifier = Modifier.size(18.dp))
        }
    }
}
