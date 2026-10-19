package de.abendlicht.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.abendlicht.app.Habit
import de.abendlicht.app.TimerState

sealed interface Panel {
    data object None : Panel
    data class Random(val poolSize: Int) : Panel
    data object Rolling : Panel
    data class Selected(val habit: Habit, val done: Boolean) : Panel
    data class Running(val habit: Habit, val timer: TimerState) : Panel
    data class Undo(val habit: Habit) : Panel
}

private fun Panel.kind() = when (this) {
    Panel.None -> 0
    is Panel.Random -> 1
    Panel.Rolling -> 1 // gleiche Fläche wie „Random“, nur der Inhalt wechselt sanft
    is Panel.Selected -> 2
    is Panel.Running -> 3
    is Panel.Undo -> 4
}

fun confirmHaptic(view: android.view.View) {
    view.performHapticFeedback(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    )
}

@Composable
fun BottomPanel(
    panel: Panel,
    onDone: (Habit) -> Unit,
    onStartTimer: (Habit) -> Unit,
    onClose: () -> Unit,
    onRoll: () -> Unit,
    onUndo: () -> Unit,
    onUnmark: (Habit) -> Unit,
    onTogglePause: () -> Unit,
    onCancelTimer: () -> Unit,
    onFinishTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 24.dp.toPx()
                shape = RoundedCornerShape(34.dp)
                clip = true
                ambientShadowColor = Color.Black.copy(alpha = 0.4f)
                spotShadowColor = Color.Black.copy(alpha = 0.4f)
            }
            .background(p.surfaceHigh)
            .clickable(remember { MutableInteractionSource() }, indication = null) { }
    ) {
        AnimatedContent(
            targetState = panel,
            contentKey = { it.kind() * 10 + if (it is Panel.Rolling) 1 else 0 },
            transitionSpec = {
                (fadeIn(calmSpring()) + scaleIn(calmSpring(), initialScale = 0.96f)) togetherWith fadeOut(calmSpring())
            },
            label = "panel",
        ) { state ->
            Box(Modifier.padding(18.dp)) {
                when (state) {
                    Panel.None -> Spacer(Modifier.height(1.dp))
                    is Panel.Random -> RandomContent(state.poolSize, onRoll, onClose)
                    Panel.Rolling -> RollingContent()
                    is Panel.Selected -> SelectedContent(state, onDone, onStartTimer, onClose, onUnmark)
                    is Panel.Running -> TimerContent(state, onTogglePause, onCancelTimer, onFinishTimer)
                    is Panel.Undo -> UndoContent(state.habit, onUndo)
                }
            }
        }
    }
}

@Composable
private fun SceneThumb(habit: Habit, size: Int = 46) {
    val c = remember(habit.scene) { sceneColors(habit.scene) }
    Canvas(Modifier.size(size.dp).clip(RoundedCornerShape(15.dp))) { drawScenePreview(habit.scene, c, 0.12f) }
}

@Composable
private fun PanelHeader(habit: Habit, subtitle: String, trailing: @Composable () -> Unit) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        SceneThumb(habit)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(habit.name, style = MaterialTheme.typography.titleLarge, color = p.ink, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = p.inkSoft)
        }
        trailing()
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    val p = LocalPalette.current
    IconButton(onClick = onClick) {
        Box(Modifier.size(34.dp).background(p.bg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, "Schließen", tint = p.inkSoft, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SelectedContent(
    s: Panel.Selected,
    onDone: (Habit) -> Unit,
    onStartTimer: (Habit) -> Unit,
    onClose: () -> Unit,
    onUnmark: (Habit) -> Unit,
) {
    val view = LocalView.current
    val sc = remember(s.habit.scene) { sceneColors(s.habit.scene) }
    Column {
        PanelHeader(s.habit, if (s.done) "Heute schon erledigt – schön!" else "Bereit für ${s.habit.minutes} Minuten?") {
            CloseButton(onClose)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!s.done) {
                PillButton(
                    "Timer · ${s.habit.minutes} Min", Icons.Rounded.Timer,
                    container = sc.top, content = sc.title, modifier = Modifier.weight(1f),
                ) { onStartTimer(s.habit) }
                PillButton(
                    "Erledigt", Icons.Rounded.Check,
                    container = sc.title, content = Color.White, modifier = Modifier.weight(1f),
                ) { confirmHaptic(view); onDone(s.habit) }
            } else {
                PillButton(
                    "Nicht erledigt", Icons.AutoMirrored.Rounded.Undo,
                    container = sc.top, content = sc.title, modifier = Modifier.weight(1f),
                ) { onUnmark(s.habit) }
            }
        }
    }
}

@Composable
private fun RandomContent(poolSize: Int, onRoll: () -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Was kommt ins Los?", style = MaterialTheme.typography.titleLarge, color = p.ink)
                Text(
                    "Tippe an, was heute nicht passt.",
                    style = MaterialTheme.typography.bodyMedium, color = p.inkSoft
                )
            }
            CloseButton(onClose)
        }
        Spacer(Modifier.height(16.dp))
        PillButton(
            if (poolSize == 0) "Nichts im Los" else "Würfeln · $poolSize im Los",
            Icons.Rounded.Casino,
            container = p.accent, content = Color.White, enabled = poolSize > 0,
            modifier = Modifier.fillMaxWidth(),
            onClick = onRoll,
        )
    }
}

@Composable
private fun RollingContent() {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Das Los fällt …", style = MaterialTheme.typography.titleLarge, color = p.ink)
        Spacer(Modifier.height(12.dp))
        WavyProgress(progress = { 1f }, playing = true, color = p.accent, trackColor = p.line, modifier = Modifier.padding(horizontal = 40.dp))
    }
}

@Composable
private fun UndoContent(habit: Habit, onUndo: () -> Unit) {
    val p = LocalPalette.current
    val sc = remember(habit.scene) { sceneColors(habit.scene) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(sc.top, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = sc.title, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Gut gemacht!", style = MaterialTheme.typography.titleMedium, color = p.ink)
            Text("${habit.name} ist für heute eingetragen.", style = MaterialTheme.typography.bodyMedium, color = p.inkSoft, maxLines = 1)
        }
        TextButton(onClick = onUndo) { Text("Rückgängig", color = p.accent, style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun TimerContent(
    s: Panel.Running,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
) {
    val p = LocalPalette.current
    val view = LocalView.current
    val sc = remember(s.habit.scene) { sceneColors(s.habit.scene) }
    val timer = s.timer
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer.running, timer.endAt) {
        now = System.currentTimeMillis()
        while (timer.running) {
            withFrameMillis { now = System.currentTimeMillis() }
            if (timer.remaining(now) <= 0L) {
                confirmHaptic(view)
                onFinish()
                break
            }
        }
    }
    val remaining = timer.remaining(now)
    val secs = (remaining + 999) / 1000
    val label = "%d:%02d".format(secs / 60, secs % 60)

    Column {
        PanelHeader(s.habit, if (timer.running) "Läuft – genieß es" else "Pausiert") { CloseButton(onCancel) }
        Spacer(Modifier.height(10.dp))
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 58.sp, lineHeight = 62.sp, fontFeatureSettings = "tnum"),
            color = sc.title.let { if (p.dark) sc.top else it },
        )
        Spacer(Modifier.height(6.dp))
        WavyProgress(
            progress = { 1f - timer.remaining(now).toFloat() / timer.totalMs.coerceAtLeast(1L) },
            playing = timer.running,
            color = if (p.dark) sc.bottom else sc.title,
            trackColor = if (p.dark) p.line else sc.top,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Abbrechen", color = p.inkSoft, style = MaterialTheme.typography.labelLarge)
            }
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val sc2 by animateFloatAsState(if (pressed) 0.92f else 1f, liftSpring(), label = "press")
            Box(
                Modifier
                    .size(68.dp)
                    .graphicsLayer { scaleX = sc2; scaleY = sc2 }
                    .clip(RoundedCornerShape(if (timer.running) 24.dp else 34.dp))
                    .background(if (p.dark) sc.bottom else sc.title)
                    .clickable(interaction, indication = null) { onTogglePause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (timer.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (timer.running) "Pause" else "Weiter",
                    tint = if (p.dark) sc.title else Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            TextButton(onClick = { confirmHaptic(view); onFinish() }, modifier = Modifier.weight(1f)) {
                Text("Fertig", color = p.accent, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight(800)))
            }
        }
    }
}

@Composable
fun PillButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, liftSpring(), label = "press")
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.35f), disabledContentColor = content.copy(alpha = 0.7f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        elevation = null,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
