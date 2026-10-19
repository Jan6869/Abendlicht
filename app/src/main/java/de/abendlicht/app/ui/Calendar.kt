package de.abendlicht.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.abendlicht.app.Habit
import de.abendlicht.app.LogEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val German = Locale.GERMAN
private val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

fun formatDay(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", German))

@Composable
fun MonthCalendar(
    log: Map<LocalDate, List<LogEntry>>,
    today: LocalDate,
    habits: List<Habit>,
    onToggle: (LocalDate, Habit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthKey)
    var dayDialog by remember { mutableStateOf<LocalDate?>(null) }

    val countThisMonth = log.keys.count { YearMonth.from(it) == month && !log[it].isNullOrEmpty() }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(p.surface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    month.month.getDisplayName(TextStyle.FULL_STANDALONE, German).replaceFirstChar { it.uppercase() } +
                        if (month.year != today.year) " ${month.year}" else "",
                    style = MaterialTheme.typography.titleLarge, color = p.ink,
                )
                Text(
                    when (countThisMonth) { 0 -> "Noch kein Abend"; 1 -> "1 Abend für dich"; else -> "$countThisMonth Abende für dich" },
                    style = MaterialTheme.typography.bodySmall, color = p.inkSoft,
                )
            }
            IconButton(onClick = { monthKey = month.minusMonths(1).toString() }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Vorheriger Monat", tint = p.inkSoft)
            }
            val canForward = month < YearMonth.from(today)
            IconButton(onClick = { if (canForward) monthKey = month.plusMonths(1).toString() }, enabled = canForward) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Nächster Monat",
                    tint = if (canForward) p.inkSoft else p.inkSoft.copy(alpha = 0.3f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = p.inkSoft.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(calmSpring()) { it / 4 * dir } + fadeIn(calmSpring())) togetherWith
                    (slideOutHorizontally(calmSpring()) { -it / 4 * dir } + fadeOut(calmSpring()))
            },
            label = "month",
        ) { m ->
            val first = m.atDay(1)
            val offset = first.dayOfWeek.value - 1
            val days = m.lengthOfMonth()
            val rows = (offset + days + 6) / 7
            Column {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val idx = r * 7 + c - offset + 1
                            Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                if (idx in 1..days) {
                                    val date = m.atDay(idx)
                                    DayCell(
                                        day = idx,
                                        entries = log[date].orEmpty(),
                                        isToday = date == today,
                                        isFuture = date > today,
                                        onClick = { dayDialog = date },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    dayDialog?.let { date ->
        val entries = log[date].orEmpty()
        val listed = habits.filter { h -> h.visible || entries.any { it.habitId == h.id } }
        AlertDialog(
            onDismissRequest = { dayDialog = null },
            containerColor = p.surfaceHigh,
            title = { Text(formatDay(date), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Was hast du an diesem Abend gemacht?", style = MaterialTheme.typography.bodyMedium, color = p.inkSoft)
                    Spacer(Modifier.height(4.dp))
                    listed.forEach { h ->
                        val on = entries.any { it.habitId == h.id }
                        val sc = sceneColors(h.scene)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (on) sc.top else p.bg)
                                .clickable { onToggle(date, h) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Box(Modifier.size(12.dp).background(sc.bottom, CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(h.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (on) sc.title else p.ink)
                            if (on) Icon(Icons.Rounded.Check, null, tint = sc.title, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dayDialog = null }) { Text("Fertig") } },
        )
    }
}

@Composable
private fun DayCell(day: Int, entries: List<LogEntry>, isToday: Boolean, isFuture: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val done = entries.isNotEmpty()
    val first = entries.firstOrNull()?.let { sceneColors(it.scene) }
    val fill by animateFloatAsState(if (done) 1f else 0f, liftSpring(), label = "fill")
    Box(
        Modifier
            .padding(3.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(if (isToday) Modifier.border(2.dp, p.accent, CircleShape) else Modifier)
            .clickable(
                enabled = !isFuture,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (first != null || fill > 0.01f) {
            val col = first?.bottom ?: p.accent
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(if (isToday) 4.dp else 1.dp)
                    .graphicsLayer { scaleX = 0.4f + 0.6f * fill; scaleY = 0.4f + 0.6f * fill; alpha = fill }
                    .background(if (p.dark) col.copy(alpha = 0.85f) else col.copy(alpha = 0.75f), CircleShape)
            )
        }
        Text(
            "$day",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (done || isToday) FontWeight(800) else FontWeight(600)),
            color = when {
                done -> first?.title ?: p.ink
                isFuture -> p.inkSoft.copy(alpha = 0.45f)
                else -> p.ink.copy(alpha = 0.8f)
            },
        )
        if (entries.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                entries.drop(1).take(3).forEach {
                    Box(Modifier.size(4.dp).background(sceneColors(it.scene).title.copy(alpha = 0.7f), CircleShape))
                }
            }
        }
    }
}

