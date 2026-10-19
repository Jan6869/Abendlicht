@file:OptIn(ExperimentalMaterial3Api::class)

package de.abendlicht.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.abendlicht.app.AppSettings
import de.abendlicht.app.Habit
import de.abendlicht.app.Scene
import de.abendlicht.app.newId
import kotlin.math.roundToInt

@Composable
fun SettingsSheet(
    habits: List<Habit>,
    settings: AppSettings,
    time: () -> Float,
    onDismiss: () -> Unit,
    onSaveHabit: (Habit) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onSettings: (AppSettings) -> Unit,
    onNeedNotificationPermission: () -> Unit,
) {
    val p = LocalPalette.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editing by remember { mutableStateOf<Habit?>(null) }
    var pickTime by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = p.bg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text("Einstellungen", style = MaterialTheme.typography.headlineSmall, color = p.ink, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(20.dp))

            SectionLabel("Deine Gewohnheiten")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                habits.forEachIndexed { i, h ->
                    HabitRow(
                        habit = h,
                        canUp = i > 0, canDown = i < habits.lastIndex,
                        onClick = { editing = h },
                        onMove = { onMove(h.id, it) },
                        onVisible = { onSaveHabit(h.copy(visible = it)) },
                    )
                }
                // Hinzufügen – gestrichelte, freundliche Fläche
                val dash = p.inkSoft.copy(alpha = 0.5f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .drawBehind {
                            drawRoundRect(
                                dash, cornerRadius = CornerRadius(22.dp.toPx()),
                                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                            )
                        }
                        .clickable { editing = Habit(newId(), "", Scene.CALM, 15) }
                        .padding(vertical = 18.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = p.inkSoft)
                    Spacer(Modifier.width(8.dp))
                    Text("Gewohnheit hinzufügen", style = MaterialTheme.typography.labelLarge, color = p.inkSoft)
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Erinnerung")
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(p.surface)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Sanft erinnern", style = MaterialTheme.typography.titleMedium, color = p.ink)
                        Text("Nur, wenn du an dem Abend noch nichts gemacht hast.", style = MaterialTheme.typography.bodyMedium, color = p.inkSoft)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.reminderOn,
                        onCheckedChange = {
                            if (it) onNeedNotificationPermission()
                            onSettings(settings.copy(reminderOn = it))
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = Color.White),
                    )
                }
                if (settings.reminderOn) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickTime = true }
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text("Uhrzeit", style = MaterialTheme.typography.titleMedium, color = p.ink, modifier = Modifier.weight(1f))
                        Text(
                            "%02d:%02d".format(settings.hour, settings.minute),
                            style = MaterialTheme.typography.titleLarge, color = p.accent,
                            modifier = Modifier
                                .background(p.accent.copy(alpha = 0.12f), CircleShape)
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Ein neuer Abend beginnt um 4 Uhr morgens – so zählt auch das späte Lesen noch zum Vortag.",
                style = MaterialTheme.typography.bodySmall, color = p.inkSoft, modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    editing?.let { h ->
        EditHabitDialog(
            initial = h,
            isNew = habits.none { it.id == h.id },
            time = time,
            onDismiss = { editing = null },
            onSave = { onSaveHabit(it); editing = null },
            onDelete = { onDeleteHabit(h.id); editing = null },
        )
    }

    if (pickTime) {
        val state = rememberTimePickerState(settings.hour, settings.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            containerColor = p.surfaceHigh,
            title = { Text("Wann soll ich erinnern?", style = MaterialTheme.typography.titleLarge) },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = p.bg,
                        selectorColor = p.accent,
                        timeSelectorSelectedContainerColor = p.accent.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = p.ink,
                        timeSelectorUnselectedContainerColor = p.bg,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSettings(settings.copy(hour = state.hour, minute = state.minute))
                    pickTime = false
                }) { Text("Speichern", color = p.accent) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Abbrechen", color = p.inkSoft) } },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val p = LocalPalette.current
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall, color = p.inkSoft,
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
    )
}

@Composable
private fun HabitRow(
    habit: Habit,
    canUp: Boolean,
    canDown: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onVisible: (Boolean) -> Unit,
) {
    val p = LocalPalette.current
    val c = remember(habit.scene) { sceneColors(habit.scene) }
    val a by animateFloatAsState(if (habit.visible) 1f else 0.45f, calmSpring(), label = "a")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(p.surface)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Canvas(
            Modifier.size(50.dp).graphicsLayer { alpha = a }.clip(RoundedCornerShape(16.dp))
        ) { drawScenePreview(habit.scene, c, 0.12f) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).graphicsLayer { alpha = a }) {
            Text(habit.name, style = MaterialTheme.typography.titleMedium, color = p.ink, maxLines = 1)
            Text("${habit.minutes} Min · ${habit.scene.label}", style = MaterialTheme.typography.bodySmall, color = p.inkSoft)
        }
        Column {
            Icon(
                Icons.Rounded.KeyboardArrowUp, "Nach oben",
                tint = if (canUp) p.inkSoft else p.inkSoft.copy(alpha = 0.2f),
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(enabled = canUp) { onMove(-1) }
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown, "Nach unten",
                tint = if (canDown) p.inkSoft else p.inkSoft.copy(alpha = 0.2f),
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(enabled = canDown) { onMove(1) }
            )
        }
        Spacer(Modifier.width(6.dp))
        Switch(
            checked = habit.visible,
            onCheckedChange = onVisible,
            colors = SwitchDefaults.colors(checkedTrackColor = c.title, checkedThumbColor = Color.White),
        )
    }
}

@Composable
private fun EditHabitDialog(
    initial: Habit,
    isNew: Boolean,
    time: () -> Float,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: () -> Unit,
) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf(initial.name) }
    var scene by remember { mutableStateOf(initial.scene) }
    var minutes by remember { mutableFloatStateOf(initial.minutes.toFloat()) }
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(p.surfaceHigh)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Text(if (isNew) "Neue Gewohnheit" else "Bearbeiten", style = MaterialTheme.typography.headlineSmall, color = p.ink, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(14.dp))

            // Live-Vorschau der Karte
            HabitCard(
                habit = initial.copy(name = name.ifBlank { "Dein Ritual" }, scene = scene, minutes = minutes.roundToInt()),
                look = CardLook.Neutral, done = false, showToggle = false, included = true,
                time = time, onClick = {}, height = 128.dp,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = p.accent, focusedLabelColor = p.accent, cursorColor = p.accent,
                    unfocusedBorderColor = p.line,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))

            Text("Bild", style = MaterialTheme.typography.labelMedium, color = p.inkSoft, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Scene.entries.forEach { s ->
                    val selected = s == scene
                    val k by animateFloatAsState(if (selected) 1f else 0f, liftSpring(), label = "sel")
                    val c = remember(s) { sceneColors(s) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Canvas(
                            Modifier
                                .size(58.dp)
                                .graphicsLayer { scaleX = 0.92f + 0.08f * k; scaleY = 0.92f + 0.08f * k }
                                .border(3.dp, p.accent.copy(alpha = k), RoundedCornerShape(19.dp))
                                .padding(4.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .clickable { scene = s }
                        ) { drawScenePreview(s, c, 0.12f) }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            s.label, style = MaterialTheme.typography.labelSmall,
                            color = if (selected) p.ink else p.inkSoft,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text("Dauer für den Timer", style = MaterialTheme.typography.labelMedium, color = p.inkSoft, modifier = Modifier.weight(1f))
                Text("${minutes.roundToInt()} Min", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight(800)), color = p.ink)
            }
            Slider(
                value = minutes,
                onValueChange = { minutes = (it / 5f).roundToInt() * 5f },
                valueRange = 5f..90f,
                colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.line),
            )
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) {
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(if (confirmDelete) "Wirklich löschen?" else "Löschen", color = Color(0xFFD0564B))
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Abbrechen", color = p.inkSoft) }
                Spacer(Modifier.width(4.dp))
                PillButton(
                    "Speichern", Icons.Rounded.Check, container = p.accent, content = Color.White,
                    enabled = name.isNotBlank(),
                ) {
                    onSave(initial.copy(name = name.trim(), scene = scene, minutes = minutes.roundToInt()))
                }
            }
        }
    }
}

