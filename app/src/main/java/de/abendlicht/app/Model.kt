package de.abendlicht.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Visuelle Welt einer Gewohnheit – bestimmt Farben und Illustration der Karte. */
enum class Scene(val label: String) {
    WALK("Spaziergang"),
    READ("Lesen"),
    JOURNAL("Schreiben"),
    STRETCH("Dehnen"),
    CALM("Ruhe"),
    GARDEN("Garten"),
}

data class Habit(
    val id: String,
    val name: String,
    val scene: Scene,
    val minutes: Int,
    val visible: Boolean = true,
)

/** Ein Eintrag im Kalender. Die Szene wird mitgespeichert, damit gelöschte Gewohnheiten ihre Farbe behalten. */
data class LogEntry(val habitId: String, val scene: Scene, val minutes: Int = -1)

data class AppSettings(
    val reminderOn: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 30,
)

data class TimerState(
    val habitId: String,
    val totalMs: Long,
    /** Wall-clock-Ende, solange der Timer läuft. */
    val endAt: Long?,
    /** Restzeit, solange der Timer pausiert ist. */
    val pausedRemainingMs: Long,
) {
    val running get() = endAt != null
    fun remaining(now: Long): Long = if (endAt != null) (endAt - now).coerceAtLeast(0) else pausedRemainingMs
}

fun formatMinutes(total: Int): String {
    val h = total / 60
    val m = total % 60
    return when {
        h == 0 -> "$m Min"
        m == 0 -> "$h Std"
        else -> "$h Std $m Min"
    }
}

const val MIN_MINUTES = 5
const val MAX_MINUTES = 90

/** Ein neuer Abend beginnt um 4 Uhr morgens – so zählt auch das Lesen um halb eins noch zum Vorabend. */
fun eveningDate(now: LocalDateTime = LocalDateTime.now()): LocalDate = now.minusHours(4).toLocalDate()

fun streakOf(log: Map<LocalDate, List<LogEntry>>, today: LocalDate): Int {
    var day = if (log[today].isNullOrEmpty()) today.minusDays(1) else today
    var n = 0
    while (!log[day].isNullOrEmpty()) {
        n++
        day = day.minusDays(1)
    }
    return n
}

fun newId(): String = UUID.randomUUID().toString().take(8)

class Store(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("abendlicht", Context.MODE_PRIVATE)

    fun loadHabits(): List<Habit> {
        val raw = prefs.getString(KEY_HABITS, null) ?: return defaultHabits()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Habit(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    scene = sceneOf(o.optString("scene")),
                    minutes = o.optInt("minutes", 15),
                    visible = o.optBoolean("visible", true),
                )
            }
        }.getOrElse { defaultHabits() }
    }

    fun saveHabits(list: List<Habit>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("name", it.name).put("scene", it.scene.name)
                    .put("minutes", it.minutes).put("visible", it.visible)
            )
        }
        prefs.edit().putString(KEY_HABITS, arr.toString()).apply()
    }

    fun loadLog(): Map<LocalDate, List<LogEntry>> {
        val raw = prefs.getString(KEY_LOG, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            val out = HashMap<LocalDate, List<LogEntry>>()
            o.keys().forEach { k ->
                val arr = o.getJSONArray(k)
                out[LocalDate.parse(k)] = (0 until arr.length()).map { i ->
                    val parts = arr.getString(i).split(":")
                    LogEntry(parts[0], sceneOf(parts.getOrNull(1)), parts.getOrNull(2)?.toIntOrNull() ?: -1)
                }
            }
            out
        }.getOrElse { emptyMap() }
    }

    fun saveLog(log: Map<LocalDate, List<LogEntry>>) {
        val o = JSONObject()
        log.forEach { (date, entries) ->
            if (entries.isNotEmpty()) {
                val arr = JSONArray()
                entries.forEach { arr.put("${it.habitId}:${it.scene.name}:${it.minutes}") }
                o.put(date.toString(), arr)
            }
        }
        prefs.edit().putString(KEY_LOG, o.toString()).apply()
    }

    fun loadExcluded(): Set<String> = prefs.getStringSet(KEY_EXCLUDED, emptySet())?.toSet() ?: emptySet()
    fun saveExcluded(set: Set<String>) = prefs.edit().putStringSet(KEY_EXCLUDED, set).apply()

    fun loadSettings() = AppSettings(
        reminderOn = prefs.getBoolean("reminderOn", false),
        hour = prefs.getInt("hour", 20),
        minute = prefs.getInt("minute", 30),
    )

    fun saveSettings(s: AppSettings) = prefs.edit()
        .putBoolean("reminderOn", s.reminderOn).putInt("hour", s.hour).putInt("minute", s.minute).apply()

    fun loadTimer(): TimerState? {
        val id = prefs.getString("timerHabit", null) ?: return null
        val end = prefs.getLong("timerEnd", -1L)
        return TimerState(
            habitId = id,
            totalMs = prefs.getLong("timerTotal", 0L),
            endAt = if (end > 0) end else null,
            pausedRemainingMs = prefs.getLong("timerRemaining", 0L),
        )
    }

    fun saveTimer(t: TimerState?) {
        val e = prefs.edit()
        if (t == null) {
            e.remove("timerHabit").remove("timerEnd").remove("timerTotal").remove("timerRemaining")
        } else {
            e.putString("timerHabit", t.habitId).putLong("timerEnd", t.endAt ?: -1L)
                .putLong("timerTotal", t.totalMs).putLong("timerRemaining", t.pausedRemainingMs)
        }
        e.apply()
    }

    private fun sceneOf(s: String?) = Scene.entries.firstOrNull { it.name == s } ?: Scene.CALM

    companion object {
        private const val KEY_HABITS = "habits"
        private const val KEY_LOG = "log"
        private const val KEY_EXCLUDED = "excluded"

        fun defaultHabits() = listOf(
            Habit("walk", "Spazieren", Scene.WALK, 20),
            Habit("read", "Lesen", Scene.READ, 20),
            Habit("journal", "Journaling", Scene.JOURNAL, 10),
            Habit("stretch", "Dehnen & Schienbeine", Scene.STRETCH, 10),
        )
    }
}
