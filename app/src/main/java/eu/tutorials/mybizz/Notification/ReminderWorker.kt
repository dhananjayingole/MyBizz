package eu.tutorials.mybizz.Notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import eu.tutorials.mybizz.Repository.BillSheetsRepository

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ReminderWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "ReminderWorker started")

        return try {
            // Create repositories
            val billSheetsRepository = BillSheetsRepository(applicationContext)
            val rentalSheetsRepository = RentalSheetsRepository(applicationContext)

            // Check reminders
            val reminderChecker = ReminderChecker(
                applicationContext,
                billSheetsRepository,
                rentalSheetsRepository
            )

            reminderChecker.checkAllReminders()

            Log.d(TAG, "ReminderWorker completed successfully")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "ReminderWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}