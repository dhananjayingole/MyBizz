package eu.tutorials.mybizz.Notification

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val WORK_NAME = "payment_reminder_work"
    }

    fun scheduleDailyReminders() {
        Log.d(TAG, "Scheduling daily payment reminders...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Run every 24 hours
        val reminderRequest = PeriodicWorkRequestBuilder<PaymentReminderWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES) // Initial delay before first run
            .addTag("payment_reminders")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )

        Log.d(TAG, "Daily payment reminders scheduled successfully")
    }

    fun scheduleImmediateCheck() {
        Log.d(TAG, "Scheduling immediate payment check...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<PaymentReminderWorker>()
            .setConstraints(constraints)
            .addTag("immediate_check")
            .build()

        WorkManager.getInstance(context).enqueue(immediateRequest)

        Log.d(TAG, "Immediate payment check scheduled")
    }

    fun cancelAllReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "All payment reminders cancelled")
    }
}