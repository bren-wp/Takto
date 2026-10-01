package hr.takto.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import hr.takto.app.data.ScheduleStore
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sinkronizira dvije neovisne vrste alarma:
 * 1) dnevni pregled u korisnički odabrano vrijeme,
 * 2) podsjetnik prije stvarnog početka sljedeće smjene.
 *
 * Za smjene držimo samo sljedeći alarm. Kad se okine, receiver zakazuje sljedeći.
 * Time izbjegavamo stotine aktivnih alarma i ostajemo stabilni i na dugim rasporedima.
 */
object ReminderScheduler {
    private const val DAILY_REQUEST_CODE = 7101
    private const val SHIFT_REQUEST_CODE = 7201

    fun sync(context: Context, store: ScheduleStore) {
        if (store.remindersEnabled.value) {
            scheduleNextDaily(context, store.reminderHour.value, store.reminderMinute.value)
        } else {
            cancelDaily(context)
        }

        if (store.shiftRemindersEnabled.value) {
            scheduleNextShift(context, store)
        } else {
            cancelShift(context)
        }
    }

    /** Zadržano kao kompatibilan naziv za postojeći receiver. */
    fun scheduleNext(context: Context, hour: Int, minute: Int) = scheduleNextDaily(context, hour, minute)

    fun scheduleNextDaily(context: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        if (!next.isAfter(now.plusSeconds(10))) next = next.plusDays(1)

        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager(context).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            dailyPendingIntent(context)
        )
    }

    fun scheduleNextShift(context: Context, store: ScheduleStore) {
        cancelShift(context)
        if (!store.shiftRemindersEnabled.value) return

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val upcoming = store.entries.values
            .asSequence()
            .filter { it.hasWorkTime && it.startMinute != null }
            .map { entry ->
                val start = entry.date
                    .atStartOfDay()
                    .plusMinutes((entry.startMinute ?: 0).toLong())
                    .atZone(zone)
                entry to start
            }
            .filter { (_, start) -> start.isAfter(now.plusSeconds(20)) }
            .minByOrNull { (_, start) -> start.toInstant() }
            ?: return

        val (entry, shiftStart) = upcoming
        val lead = store.shiftReminderLeadMinutes.value.coerceAtLeast(0).toLong()
        val desired = shiftStart.minusMinutes(lead)
        // Ako je uređaj bio ugašen baš u trenutku podsjetnika, upozori čim prije
        // umjesto da potpuno preskočimo nadolazeću smjenu.
        val trigger = if (desired.isAfter(now.plusSeconds(10))) desired else now.plusSeconds(10)

        val triggerAt = trigger.toInstant().toEpochMilli()
        alarmManager(context).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            shiftPendingIntent(
                context = context,
                date = entry.date.toString(),
                expectedStartMinute = entry.startMinute ?: -1
            )
        )
    }

    /** Otkazuje samo dnevni pregled; podsjetnik prije smjene ostaje neovisan. */
    fun cancelDaily(context: Context) {
        alarmManager(context).cancel(dailyPendingIntent(context))
    }

    /** Otkazuje samo alarm sljedeće smjene. */
    fun cancelShift(context: Context) {
        alarmManager(context).cancel(shiftPendingIntent(context, null, -1))
    }

    /** Otkazuje sve Takto alarme. */
    fun cancel(context: Context) {
        cancelDaily(context)
        cancelShift(context)
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun dailyPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        DAILY_REQUEST_CODE,
        Intent(context, DailyReminderReceiver::class.java)
            .setAction(DailyReminderReceiver.ACTION_DAILY_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun shiftPendingIntent(
        context: Context,
        date: String?,
        expectedStartMinute: Int
    ): PendingIntent {
        val intent = Intent(context, ShiftReminderReceiver::class.java)
            .setAction(ShiftReminderReceiver.ACTION_SHIFT_REMINDER)
        if (date != null) {
            intent.putExtra(ShiftReminderReceiver.EXTRA_DATE, date)
            intent.putExtra(ShiftReminderReceiver.EXTRA_EXPECTED_START_MINUTE, expectedStartMinute)
        }
        return PendingIntent.getBroadcast(
            context,
            SHIFT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
