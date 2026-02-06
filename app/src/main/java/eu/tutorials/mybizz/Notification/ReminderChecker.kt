package eu.tutorials.mybizz.Notification

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import eu.tutorials.mybizz.Logic.Bill.BillRepository
import eu.tutorials.mybizz.Logic.Rental.RentalRepository
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.Repository.BillSheetsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReminderChecker(
    private val context: Context,
    private val billSheetsRepository: BillSheetsRepository,
    private val rentalSheetsRepository: RentalSheetsRepository
) {
    companion object {
        private const val TAG = "ReminderChecker"

        // Configuration
        private val REMIND_DAYS_BEFORE = intArrayOf(0, 1, 3, 7) // Today, 1 day, 3 days, 1 week before
    }

    private val notificationHelper = NotificationHelper(context)
    private val billRepository = BillRepository()
    private val rentalRepository = RentalRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Date formatters
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    fun checkAllReminders() {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Starting reminder check...")

                // Check bills
                val bills = billRepository.getAllBills(billSheetsRepository)
                checkBillsReminders(bills)

                // Check rentals
                val rentals = getAllRentals()
                checkRentalsReminders(rentals)

                Log.d(TAG, "Reminder check completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking reminders: ${e.message}", e)
            }
        }
    }

    private suspend fun getAllRentals(): List<Rental> {
        return try {
            RentalRepository().getAllRentals(rentalSheetsRepository)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching rentals: ${e.message}")
            emptyList()
        }
    }

    private suspend fun checkBillsReminders(bills: List<Bill>) {
        val today = Date()
        val calendar = Calendar.getInstance()
        calendar.time = today

        // Set time to 00:00:00 for accurate day comparison
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayNormalized = calendar.time

        bills.forEach { bill ->
            if (bill.status == Bill.STATUS_UNPAID && bill.dueDate.isNotBlank()) {
                try {
                    val dueDate = dateFormat.parse(bill.dueDate)
                    if (dueDate != null) {
                        val daysUntilDue = calculateDaysBetween(todayNormalized, dueDate)

                        // Check if we should notify for this day
                        if (REMIND_DAYS_BEFORE.contains(daysUntilDue)) {
                            notificationHelper.showBillReminder(
                                billTitle = bill.title,
                                billAmount = bill.amount,
                                dueDate = bill.dueDate,
                                daysUntilDue = daysUntilDue.toLong(),
                                billId = bill.id
                            )
                            Log.d(TAG, "Scheduled bill reminder: ${bill.title} due in $daysUntilDue days")
                        }

                        // Special overdue notification (after due date)
                        if (daysUntilDue < 0) {
                            notificationHelper.showOverdueReminder(
                                title = "Bill: ${bill.title}",
                                message = "$${bill.amount} was due on ${bill.dueDate} - ${-daysUntilDue} days overdue!",
                                itemId = bill.id
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse date for bill ${bill.id}: ${bill.dueDate}")
                }
            }
        }
    }

    private suspend fun checkRentalsReminders(rentals: List<Rental>) {
        val today = Date()
        val calendar = Calendar.getInstance()
        calendar.time = today
        val currentMonth = monthFormat.format(today)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        rentals.forEach { rental ->
            if (rental.status == Rental.STATUS_UNPAID && rental.month.isNotBlank()) {
                try {
                    // Parse month (format: "2025-10")
                    val rentalMonthDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(rental.month)

                    if (rentalMonthDate != null) {
                        val rentalCalendar = Calendar.getInstance()
                        rentalCalendar.time = rentalMonthDate

                        // Assume rent is due on the 5th of each month
                        rentalCalendar.set(Calendar.DAY_OF_MONTH, 5)
                        val dueDate = rentalCalendar.time

                        // If current month's rent, calculate days until due
                        if (rental.month == currentMonth) {
                            val daysUntilDue = calculateDaysBetween(today, dueDate)

                            if (REMIND_DAYS_BEFORE.contains(daysUntilDue)) {
                                notificationHelper.showRentalReminder(
                                    tenantName = rental.tenantName,
                                    property = rental.property,
                                    rentAmount = rental.rentAmount,
                                    month = rental.month,
                                    daysUntilDue = daysUntilDue.toLong(),
                                    rentalId = rental.id
                                )
                                Log.d(TAG, "Scheduled rental reminder: ${rental.tenantName} due in $daysUntilDue days")
                            }
                        }

                        // Check if overdue (past the 5th of current month)
                        if (rental.month <= currentMonth && currentDay > 5) {
                            notificationHelper.showOverdueReminder(
                                title = "Rent: ${rental.tenantName}",
                                message = "$${rental.rentAmount} for ${rental.month} was due on the 5th - ${currentDay - 5} days overdue!",
                                itemId = rental.id
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to process rental ${rental.id}: ${e.message}")
                }
            }
        }
    }

    private fun calculateDaysBetween(date1: Date, date2: Date): Int {
        val diff = date2.time - date1.time
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }

    fun clearAllReminders() {
        // Clear all notifications
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
}