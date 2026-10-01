package hr.takto.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import hr.takto.app.data.ScheduleStore
import hr.takto.app.reminders.DailyReminderReceiver
import hr.takto.app.reminders.ReminderScheduler
import hr.takto.app.reminders.ShiftReminderReceiver

class TaktoApplication : Application() {
    lateinit var scheduleStore: ScheduleStore
        private set

    override fun onCreate() {
        super.onCreate()
        scheduleStore = ScheduleStore(this)
        createNotificationChannels()
        ReminderScheduler.sync(this, scheduleStore)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val daily = NotificationChannel(
            DailyReminderReceiver.CHANNEL_ID,
            "Dnevni raspored",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Podsjetnik za današnju smjenu ili obvezu u Takto rasporedu."
        }
        val shifts = NotificationChannel(
            ShiftReminderReceiver.CHANNEL_ID,
            "Podsjetnici prije smjene",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Obavijest prije početka smjene s upisanim radnim vremenom."
        }
        manager.createNotificationChannel(daily)
        manager.createNotificationChannel(shifts)
    }
}
