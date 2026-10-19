package de.abendlicht.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.ZoneId

object Alarms {
    const val ACTION_REMINDER = "de.abendlicht.app.REMINDER"
    const val ACTION_TIMER = "de.abendlicht.app.TIMER"
    private const val CH_REMINDER = "reminder"
    private const val CH_TIMER = "timer"
    const val NOTIF_REMINDER = 1
    const val NOTIF_TIMER = 2

    private fun pending(context: Context, action: String, code: Int): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    private fun schedule(context: Context, at: Long, pi: PendingIntent, precise: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (precise && canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, at, if (precise) 30_000L else 5 * 60_000L, pi)
        }
    }

    fun scheduleReminder(context: Context, settings: AppSettings) {
        val pi = pending(context, ACTION_REMINDER, 10)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pi)
        if (!settings.reminderOn) return
        val now = LocalDateTime.now()
        var next = now.withHour(settings.hour).withMinute(settings.minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        schedule(context, next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pi, precise = false)
    }

    fun scheduleTimer(context: Context, endAt: Long) =
        schedule(context, endAt, pending(context, ACTION_TIMER, 11), precise = true)

    fun cancelTimer(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context, ACTION_TIMER, 11))
        NotificationManagerCompat.from(context).cancel(NOTIF_TIMER)
    }

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDER, "Abend-Erinnerung", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Eine sanfte Erinnerung am Abend" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_TIMER, "Timer", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Wenn ein Timer abgelaufen ist" }
        )
    }

    @SuppressLint("MissingPermission")
    fun notify(context: Context, id: Int, channelTimer: Boolean, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, if (channelTimer) CH_TIMER else CH_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE8875B.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = Store(context)
        when (intent.action) {
            Alarms.ACTION_REMINDER -> {
                // Nur erinnern, wenn heute noch nichts erledigt ist.
                val done = !store.loadLog()[eveningDate()].isNullOrEmpty()
                if (!done) {
                    Alarms.notify(
                        context, Alarms.NOTIF_REMINDER, false,
                        "Zeit für deinen Abend", "Ein kleiner Moment nur für dich wartet. 🌙"
                    )
                }
                Alarms.scheduleReminder(context, store.loadSettings())
            }
            Alarms.ACTION_TIMER -> {
                val timer = store.loadTimer() ?: return
                if (!timer.running) return
                val habit = store.loadHabits().firstOrNull { it.id == timer.habitId }
                Alarms.notify(
                    context, Alarms.NOTIF_TIMER, true,
                    "Geschafft! ✨", "${habit?.name ?: "Deine Gewohnheit"} – die Zeit ist um. Schön gemacht."
                )
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Alarms.scheduleReminder(context, Store(context).loadSettings())
    }
}
