package hr.takto.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import hr.takto.app.data.ScheduleStore

/** Ponovno sinkronizira dnevni i smjenski alarm nakon događaja koji mogu promijeniti vrijeme okidanja. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            ReminderScheduler.sync(context, ScheduleStore(context))
        }
    }
}
