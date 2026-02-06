package eu.tutorials.mybizz.Notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    companion object {
        private const val WORK_NAME = "daily_reminder_check"
    }

    fun scheduleDailyReminders() {
        // Cancel any existing work first
        cancelAllReminders()

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, // Repeat interval
            TimeUnit.HOURS
        )
            .setInitialDelay(5, TimeUnit.MINUTES) // Start after 5 minutes
            .addTag("reminders")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE, // Replace existing schedule
            workRequest
        )
    }

    fun cancelAllReminders() {
        WorkManager.getInstance(context).cancelAllWorkByTag("reminders")
    }
}