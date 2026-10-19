package de.abendlicht.app.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.abendlicht.app.AppViewModel
import de.abendlicht.app.Mode
import java.time.LocalTime

@Composable
fun HomeScreen(vm: AppViewModel, requestNotificationPermission: () -> Unit) {
    val p = LocalPalette.current
    val view = LocalView.current
    val reduce = LocalReduceMotion.current
    var showSettings by remember { mutableStateOf(false) }

    // Eine gemeinsame, langsame Uhr für alle Illustrationen. Gelesen wird sie nur beim Zeichnen.
    val inf = rememberInfiniteTransition(label = "scenes")
    val clock = inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing)),
        label = "t",
    )
    val time: () -> Float = { if (reduce) 0.12f else clock.value }

    LaunchedEffect(vm.rollTick) { if (vm.rollTick > 0) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    LaunchedEffect(vm.landedTick) { if (vm.landedTick > 0) confirmHaptic(view) }

    BackHandler(enabled = vm.mode != Mode.Browse || (vm.selectedId != null && vm.timer == null)) { vm.clearSelection() }

    val habits = vm.visibleHabits
    val focusId = when (vm.mode) {
        Mode.Rolling -> vm.rollingId
        else -> vm.selectedId
    }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .clickable(remember { MutableInteractionSource() }, indication = null) { vm.clearSelection() }
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Header(onSettings = { showSettings = true }, settingsOpen = showSettings)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StreakChip(vm.streak, vm.log[vm.today].isNullOrEmpty().not())
                Spacer(Modifier.weight(1f))
                RandomButton(active = vm.mode != Mode.Browse, rolling = vm.mode == Mode.Rolling) {
                    if (vm.mode == Mode.Browse) vm.enterRandom() else vm.clearSelection()
                }
            }
            Spacer(Modifier.height(18.dp))

            if (habits.isEmpty()) {
                Text(
                    "Noch keine Gewohnheiten sichtbar. Über das Zahnrad kannst du welche hinzufügen.",
                    style = MaterialTheme.typography.bodyLarge, color = p.inkSoft,
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                habits.forEach { h ->
                    val excluded = h.id in vm.excluded
                    val look = when {
                        vm.mode == Mode.RandomPick -> if (excluded) CardLook.Excluded else CardLook.Neutral
                        vm.mode == Mode.Rolling -> when {
                            excluded -> CardLook.Excluded
                            h.id == focusId -> CardLook.Focused
                            else -> CardLook.Receded
                        }
                        focusId == null -> CardLook.Neutral
                        h.id == focusId -> CardLook.Focused
                        else -> CardLook.Receded
                    }
                    HabitCard(
                        habit = h,
                        look = look,
                        done = vm.doneToday(h.id),
                        showToggle = vm.mode == Mode.RandomPick,
                        included = !excluded,
                        time = time,
                        onClick = { vm.tapCard(h.id) },
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            MonthCalendar(
                log = vm.log,
                today = vm.today,
                habits = vm.habits,
                onToggle = { d, h -> vm.toggleOnDate(d, h) },
            )
            // Platz, damit das Panel unten nie etwas verdeckt
            Spacer(Modifier.height(if (vm.timer != null) 330.dp else 180.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }

        // Weicher Übergang am unteren Rand statt harter Kante
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(p.bg.copy(alpha = 0f), p.bg.copy(alpha = 0.92f))))
        )

        val panel: Panel = run {
            val t = vm.timer
            val sel = vm.selectedId?.let { id -> vm.habits.firstOrNull { it.id == id } }
            val undo = vm.lastDone?.second?.let { id -> vm.habits.firstOrNull { it.id == id } }
            when {
                t != null -> vm.habits.firstOrNull { it.id == t.habitId }?.let { Panel.Running(it, t) } ?: Panel.None
                vm.mode == Mode.Rolling -> Panel.Rolling
                vm.mode == Mode.RandomPick -> Panel.Random(habits.count { it.id !in vm.excluded })
                sel != null -> Panel.Selected(sel, vm.doneToday(sel.id))
                undo != null -> Panel.Undo(undo)
                else -> Panel.None
            }
        }
        // Beim Ausblenden soll der letzte Inhalt sichtbar bleiben, bis das Panel verschwunden ist.
        val lastPanel = remember { arrayOf<Panel>(Panel.None) }
        if (panel != Panel.None) lastPanel[0] = panel

        AnimatedVisibility(
            visible = panel != Panel.None,
            enter = slideInVertically(liftSpring()) { it } + fadeIn(calmSpring()),
            exit = slideOutVertically(calmSpring()) { it } + fadeOut(calmSpring()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BottomPanel(
                panel = if (panel == Panel.None) lastPanel[0] else panel,
                onDone = { vm.markDone(it.id) },
                onStartTimer = { requestNotificationPermission(); vm.startTimer(it.id) },
                onClose = { vm.clearSelection() },
                onRoll = { vm.roll() },
                onUndo = { vm.undoDone() },
                onUnmark = { vm.unmark(vm.today, it.id) },
                onTogglePause = { vm.togglePause() },
                onCancelTimer = { vm.cancelTimer() },
                onFinishTimer = { vm.finishTimer() },
            )
        }
    }

    if (showSettings) {
        SettingsSheet(
            habits = vm.habits,
            settings = vm.settings,
            time = time,
            onDismiss = { showSettings = false },
            onSaveHabit = { vm.saveHabit(it) },
            onDeleteHabit = { vm.deleteHabit(it) },
            onMove = { id, d -> vm.moveHabit(id, d) },
            onSettings = { vm.updateSettings(it) },
            onNeedNotificationPermission = requestNotificationPermission,
        )
    }
}

@Composable
private fun Header(onSettings: () -> Unit, settingsOpen: Boolean) {
    val p = LocalPalette.current
    val hour = remember { LocalTime.now().hour }
    val greeting = when (hour) {
        in 4..10 -> "Guten Morgen"
        in 11..16 -> "Hallo du"
        else -> "Guten Abend"
    }
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(greeting, style = MaterialTheme.typography.displaySmall, color = p.ink)
            Spacer(Modifier.height(2.dp))
            Text(
                formatDay(java.time.LocalDate.now()),
                style = MaterialTheme.typography.bodyLarge, color = p.inkSoft,
            )
            Spacer(Modifier.height(10.dp))
            Squiggle(p.accent.copy(alpha = 0.55f), Modifier.width(46.dp).height(8.dp))
        }
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val turn by animateFloatAsState(if (settingsOpen) 90f else 0f, liftSpring(), label = "turn")
        val scale by animateFloatAsState(if (pressed) 0.9f else 1f, liftSpring(), label = "press")
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(46.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(p.surface)
                .clickable(interaction, indication = null, onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Settings, "Einstellungen", tint = p.inkSoft,
                modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = turn }
            )
        }
    }
}

@Composable
private fun StreakChip(streak: Int, doneToday: Boolean) {
    val p = LocalPalette.current
    val warm = Color(0xFFF29A4A)
    val glow by animateFloatAsState(if (doneToday) 1f else 0f, calmSpring(), label = "glow")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(p.surface)
            .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Box(
            Modifier.size(30.dp).background(warm.copy(alpha = 0.14f + 0.2f * glow), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.LocalFireDepartment, null,
                tint = if (streak > 0) warm else p.inkSoft.copy(alpha = 0.6f),
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        AnimatedContent(
            targetState = streak,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(liftSpring()) { if (up) it else -it } + fadeIn()) togetherWith
                    (slideOutVertically(calmSpring()) { if (up) -it else it } + fadeOut())
            },
            label = "streak",
        ) { n ->
            Text(
                "$n",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight(600), fontSize = 20.sp),
                color = p.ink,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            when {
                streak == 0 -> "Starte heute"
                streak == 1 -> "Abend"
                else -> "Abende in Folge"
            },
            style = MaterialTheme.typography.bodyMedium, color = p.inkSoft,
        )
    }
}

@Composable
private fun RandomButton(active: Boolean, rolling: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, liftSpring(), label = "press")
    val on by animateFloatAsState(if (active) 1f else 0f, calmSpring(), label = "on")
    val spinT = rememberInfiniteTransition(label = "spin")
    val spin by spinT.animateFloat(0f, 360f, infiniteRepeatable(tween(700, easing = LinearEasing)), label = "spin")
    val bg = lerpColor(p.surface, p.accent, on)
    val fg = lerpColor(p.ink, Color.White, on)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(start = 14.dp, end = 18.dp, top = 11.dp, bottom = 11.dp)
    ) {
        Icon(
            Icons.Rounded.Casino, null, tint = fg,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = if (rolling) spin else 0f }
        )
        Spacer(Modifier.width(8.dp))
        Text("Zufall", style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

private fun lerpColor(a: Color, b: Color, f: Float) = androidx.compose.ui.graphics.lerp(a, b, f.coerceIn(0f, 1f))
