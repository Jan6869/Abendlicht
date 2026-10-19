package de.abendlicht.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

enum class Mode { Browse, RandomPick, Rolling }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    var habits by mutableStateOf(store.loadHabits()); private set
    // Alte Einträge ohne gespeicherte Dauer bekommen die Dauer ihrer Gewohnheit.
    var log by mutableStateOf(
        store.loadLog().mapValues { (_, list) ->
            list.map { e ->
                if (e.minutes >= 0) e else e.copy(minutes = habits.firstOrNull { it.id == e.habitId }?.minutes ?: 0)
            }
        }
    ); private set
    var excluded by mutableStateOf(store.loadExcluded()); private set
    var settings by mutableStateOf(store.loadSettings()); private set
    var timer by mutableStateOf(store.loadTimer()); private set

    var today by mutableStateOf(eveningDate()); private set
    var mode by mutableStateOf(Mode.Browse); private set
    var selectedId by mutableStateOf<String?>(null); private set
    /** Karte, die während des Würfelns gerade „aufleuchtet“. */
    var rollingId by mutableStateOf<String?>(null); private set
    /** Zählt hoch bei jedem Würfel-Schritt – die UI gibt dafür ein haptisches Ticken. */
    var rollTick by mutableIntStateOf(0); private set
    var landedTick by mutableIntStateOf(0); private set
    /** Für „Rückgängig“ nach dem Abhaken. */
    var lastDone by mutableStateOf<Pair<LocalDate, String>?>(null); private set

    private var rollJob: Job? = null
    private var undoJob: Job? = null

    init {
        // Falls die App geschlossen war, während ein Timer lief.
        timer?.let { selectedId = it.habitId }
        reconcileTimer()
    }

    val visibleHabits get() = habits.filter { it.visible }
    val streak get() = streakOf(log, today)
    fun doneToday(id: String) = log[today].orEmpty().any { it.habitId == id }

    fun onResume() {
        today = eveningDate()
        reconcileTimer()
    }

    // ── Auswahl ───────────────────────────────────────────────

    fun tapCard(id: String) {
        when (mode) {
            Mode.Browse -> {
                if (timer != null) return
                selectedId = if (selectedId == id) null else id
            }
            Mode.RandomPick -> toggleExcluded(id)
            Mode.Rolling -> Unit
        }
    }

    fun clearSelection() {
        if (timer != null) return
        rollJob?.cancel()
        mode = Mode.Browse
        selectedId = null
        rollingId = null
    }

    fun enterRandom() {
        if (timer != null) return
        selectedId = null
        mode = Mode.RandomPick
    }

    private fun toggleExcluded(id: String) {
        excluded = if (id in excluded) excluded - id else excluded + id
        store.saveExcluded(excluded)
    }

    fun roll() {
        val pool = visibleHabits.filter { it.id !in excluded }
        if (pool.isEmpty()) return
        val target = pool[Random.nextInt(pool.size)]
        mode = Mode.Rolling
        rollJob?.cancel()
        rollJob = viewModelScope.launch {
            if (pool.size > 1) {
                // Ein paar Runden, die sich wie ein auslaufendes Glücksrad verlangsamen.
                val start = Random.nextInt(pool.size)
                val targetIndex = pool.indexOf(target)
                val rounds = if (pool.size <= 2) 4 else 2
                var steps = rounds * pool.size + ((targetIndex - start + pool.size) % pool.size)
                if (steps < 8) steps += pool.size
                for (i in 0..steps) {
                    rollingId = pool[(start + i) % pool.size].id
                    rollTick++
                    val f = i.toFloat() / steps
                    delay((70 + 330 * f * f * f).toLong())
                }
                delay(160)
            }
            rollingId = null
            selectedId = target.id
            mode = Mode.Browse
            landedTick++
        }
    }

    // ── Erledigt ──────────────────────────────────────────────

    /** Insgesamt genommene Zeit in Minuten. */
    val totalMinutes get() = log.values.sumOf { list -> list.sumOf { it.minutes.coerceAtLeast(0) } }
    fun minutesIn(month: java.time.YearMonth) = log.entries
        .filter { java.time.YearMonth.from(it.key) == month }
        .sumOf { (_, list) -> list.sumOf { it.minutes.coerceAtLeast(0) } }

    /**
     * Trägt die Gewohnheit für heute ein. `minutes` kommt vom Timer (tatsächlich genommene Zeit);
     * ohne Angabe zählt die eingestellte Dauer. Läuft ein Timer für etwas schon Erledigtes, wird die Zeit addiert.
     */
    fun markDone(id: String, minutes: Int? = null) {
        val habit = habits.firstOrNull { it.id == id } ?: return
        val entries = log[today].orEmpty()
        val existing = entries.firstOrNull { it.habitId == id }
        if (existing == null) {
            log = log + (today to (entries + LogEntry(id, habit.scene, minutes ?: habit.minutes)))
            store.saveLog(log)
        } else if (minutes != null) {
            log = log + (today to entries.map { if (it.habitId == id) it.copy(minutes = it.minutes + minutes) else it })
            store.saveLog(log)
        }
        selectedId = null
        lastDone = today to id
        undoJob?.cancel()
        undoJob = viewModelScope.launch { delay(5000); lastDone = null }
    }

    fun undoDone() {
        val (date, id) = lastDone ?: return
        unmark(date, id)
        lastDone = null
    }

    fun unmark(date: LocalDate, id: String) {
        val rest = log[date].orEmpty().filterNot { it.habitId == id }
        log = if (rest.isEmpty()) log - date else log + (date to rest)
        store.saveLog(log)
    }

    /** Im Kalender nachtragen oder entfernen. */
    fun toggleOnDate(date: LocalDate, habit: Habit) {
        val entries = log[date].orEmpty()
        if (entries.any { it.habitId == habit.id }) {
            unmark(date, habit.id)
        } else {
            log = log + (date to (entries + LogEntry(habit.id, habit.scene, habit.minutes)))
            store.saveLog(log)
        }
    }

    // ── Timer ─────────────────────────────────────────────────

    fun startTimer(id: String, minutes: Int? = null) {
        val habit = habits.firstOrNull { it.id == id } ?: return
        val m = (minutes ?: habit.minutes).coerceIn(MIN_MINUTES, MAX_MINUTES)
        if (m != habit.minutes) saveHabit(habit.copy(minutes = m))
        val total = m * 60_000L
        val t = TimerState(id, total, System.currentTimeMillis() + total, total)
        updateTimer(t)
        Alarms.scheduleTimer(getApplication(), t.endAt!!)
    }

    fun togglePause() {
        val t = timer ?: return
        val now = System.currentTimeMillis()
        if (t.running) {
            updateTimer(t.copy(endAt = null, pausedRemainingMs = t.remaining(now)))
            Alarms.cancelTimer(getApplication())
        } else {
            val end = now + t.pausedRemainingMs
            updateTimer(t.copy(endAt = end))
            Alarms.scheduleTimer(getApplication(), end)
        }
    }

    fun cancelTimer() {
        updateTimer(null)
        Alarms.cancelTimer(getApplication())
    }

    /** Timer abgelaufen oder vorzeitig „Fertig“ gedrückt. */
    fun finishTimer() {
        val t = timer ?: return
        updateTimer(null)
        Alarms.cancelTimer(getApplication())
        val elapsed = t.totalMs - t.remaining(System.currentTimeMillis())
        markDone(t.habitId, ((elapsed + 30_000L) / 60_000L).toInt().coerceAtLeast(1))
    }

    fun reconcileTimer() {
        val t = timer ?: return
        if (habits.none { it.id == t.habitId }) { cancelTimer(); return }
        if (t.running && t.remaining(System.currentTimeMillis()) <= 0) finishTimer()
    }

    private fun updateTimer(t: TimerState?) {
        timer = t
        store.saveTimer(t)
    }

    // ── Einstellungen ─────────────────────────────────────────

    fun saveHabit(h: Habit) {
        habits = if (habits.any { it.id == h.id }) habits.map { if (it.id == h.id) h else it } else habits + h
        store.saveHabits(habits)
    }

    /** Neue Standarddauer, z. B. nach dem Wischen über den Timer-Knopf. */
    fun setMinutes(id: String, minutes: Int) {
        val h = habits.firstOrNull { it.id == id } ?: return
        val m = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
        if (m != h.minutes) saveHabit(h.copy(minutes = m))
    }

    fun deleteHabit(id: String) {
        habits = habits.filterNot { it.id == id }
        store.saveHabits(habits)
        if (selectedId == id) selectedId = null
        if (timer?.habitId == id) cancelTimer()
    }

    fun moveHabit(id: String, delta: Int) {
        val i = habits.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in habits.indices) return
        habits = habits.toMutableList().apply { add(j, removeAt(i)) }
        store.saveHabits(habits)
    }

    fun updateSettings(s: AppSettings) {
        settings = s
        store.saveSettings(s)
        Alarms.scheduleReminder(getApplication(), s)
    }
}
