package hr.takto.app.reminders

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import hr.takto.app.MainActivity
import hr.takto.app.R
import hr.takto.app.data.ScheduleStore
import hr.takto.app.model.ScheduleLogic
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Obavijest prije početka konkretne smjene. */
class ShiftReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_SHIFT_REMINDER) return

        val store = ScheduleStore(context)
        if (!store.shiftRemindersEnabled.value) {
            ReminderScheduler.cancelShift(context)
            return
        }

        val date = intent.getStringExtra(EXTRA_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val expectedStart = intent.getIntExtra(EXTRA_EXPECTED_START_MINUTE, -1)
        val entry = date?.let(store::entryFor)

        // Ako je korisnik u međuvremenu promijenio ili obrisao smjenu, stari alarm
        // ne prikazuje netočnu obavijest.
        if (
            date != null &&
            entry?.hasWorkTime == true &&
            entry.startMinute == expectedStart &&
            notificationsAllowed(context)
        ) {
            val openApp = PendingIntent.getActivity(
                context,
                7202,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_DATE, date.toString())
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val dateText = date.format(DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.forLanguageTag("hr")))
            val timeText = "${ScheduleLogic.formatClock(entry.startMinute)}–${ScheduleLogic.formatClock(entry.endMinute)}"
            val details = buildString {
                append(entry.code)
                if (entry.label.isNotBlank() && !entry.label.equals(entry.code, ignoreCase = true)) {
                    append(" · ").append(entry.label)
                }
                append(" · ").append(timeText)
                if (entry.note.isNotBlank()) append(" — ").append(entry.note)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Smjena uskoro · $dateText")
                .setContentText(details)
                .setStyle(NotificationCompat.BigTextStyle().bigText(details))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }

        // Bez obzira je li trenutna obavijest prikazana, nastavi lanac na sljedeću smjenu.
        ReminderScheduler.scheduleNextShift(context, store)
    }

    private fun notificationsAllowed(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val ACTION_SHIFT_REMINDER = "hr.takto.app.action.SHIFT_REMINDER"
        const val EXTRA_DATE = "hr.takto.app.extra.SHIFT_DATE"
        const val EXTRA_EXPECTED_START_MINUTE = "hr.takto.app.extra.SHIFT_START_MINUTE"
        const val CHANNEL_ID = "takto_shift_reminders"
        private const val NOTIFICATION_ID = 7203
    }
}
