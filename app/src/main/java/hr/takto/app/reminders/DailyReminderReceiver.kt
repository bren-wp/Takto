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

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DAILY_REMINDER) return

        val store = ScheduleStore(context)
        if (!store.remindersEnabled.value) {
            ReminderScheduler.cancelDaily(context)
            return
        }

        val entry = store.entryFor(LocalDate.now())
        if (entry != null && notificationsAllowed(context)) {
            val openApp = PendingIntent.getActivity(
                context,
                7102,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_DATE, LocalDate.now().toString())
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val details = buildString {
                append(entry.code)
                if (entry.label.isNotBlank() && !entry.label.equals(entry.code, ignoreCase = true)) {
                    append(" · ").append(entry.label)
                }
                if (entry.hasWorkTime) {
                    append(" · ")
                        .append(ScheduleLogic.formatClock(entry.startMinute))
                        .append("–")
                        .append(ScheduleLogic.formatClock(entry.endMinute))
                }
                if (entry.note.isNotBlank()) append(" — ").append(entry.note)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Današnji raspored")
                .setContentText(details)
                .setStyle(NotificationCompat.BigTextStyle().bigText(details))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }

        ReminderScheduler.scheduleNext(context, store.reminderHour.value, store.reminderMinute.value)
    }

    private fun notificationsAllowed(context: Context): Boolean {
        return android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "hr.takto.app.action.DAILY_REMINDER"
        const val CHANNEL_ID = "takto_daily_schedule"
        private const val NOTIFICATION_ID = 7103
    }
}
