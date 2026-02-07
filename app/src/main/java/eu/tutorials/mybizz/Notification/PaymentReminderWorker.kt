package eu.tutorials.mybizz.Notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.tutorials.mybizz.Logic.Bill.BillRepository
import eu.tutorials.mybizz.Logic.Rental.RentalRepository
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.Repository.BillSheetsRepository
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PaymentReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "PaymentReminderWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting payment reminder check...")

            // Check if we have notification permission
            if (!hasNotificationPermission()) {
                Log.w(TAG, "No notification permission. Skipping reminder check.")
                return@withContext Result.success()
            }

            val billSheetsRepo = BillSheetsRepository(applicationContext)
            val rentalSheetsRepo = RentalSheetsRepository(applicationContext)
            val billRepo = BillRepository()

            // Check unpaid bills
            val bills = billRepo.getAllBills(billSheetsRepo)
            val unpaidBills = bills.filter { it.status == Bill.STATUS_UNPAID }

            Log.d(TAG, "Found ${unpaidBills.size} unpaid bills")

            unpaidBills.forEachIndexed { index, bill ->
                try {
                    val daysUntilDue = calculateDaysUntilDue(bill.dueDate)
                    val message = when {
                        daysUntilDue < 0 -> "Overdue by ${-daysUntilDue} days! Amount: ₹${bill.amount}"
                        daysUntilDue == 0 -> "Due TODAY! Amount: ₹${bill.amount}"
                        daysUntilDue <= 3 -> "Due in $daysUntilDue days. Amount: ₹${bill.amount}"
                        else -> "Amount: ₹${bill.amount}. Due: ${bill.dueDate}"
                    }

                    sendNotificationSafely(
                        notificationId = 1000 + index,
                        title = "Payment Reminder: ${bill.title}",
                        message = message,
                        type = "bill"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing bill notification: ${e.message}")
                }
            }

            // Check unpaid rentals
            val rentals = rentalSheetsRepo.getAllRentals()
            val unpaidRentals = rentals.filter { it.status == Rental.STATUS_UNPAID }

            Log.d(TAG, "Found ${unpaidRentals.size} unpaid rentals")

            unpaidRentals.forEachIndexed { index, rental ->
                try {
                    val message = "Rent for ${rental.month} - ${rental.property}. Amount: ₹${rental.rentAmount}"

                    sendNotificationSafely(
                        notificationId = 2000 + index,
                        title = "Rent Reminder: ${rental.tenantName}",
                        message = message,
                        type = "rental"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing rental notification: ${e.message}")
                }
            }

            Log.d(TAG, "Payment reminder check completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking payment reminders: ${e.message}", e)
            Result.retry()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed for Android 12 and below
        }
    }

    private fun sendNotificationSafely(
        notificationId: Int,
        title: String,
        message: String,
        type: String
    ) {
        try {
            // Double-check permission before sending
            if (hasNotificationPermission()) {
                NotificationHelper.sendPaymentReminder(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = title,
                    message = message,
                    type = type
                )
            } else {
                Log.w(TAG, "Notification permission not granted. Skipping notification.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when sending notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification: ${e.message}")
        }
    }

    private fun calculateDaysUntilDue(dueDate: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val due = sdf.parse(dueDate) ?: return 999
            val today = Calendar.getInstance().time

            val diffInMillis = due.time - today.time
            val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

            diffInDays
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating days until due: ${e.message}")
            999
        }
    }
}