package eu.tutorials.mybizz.Notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import eu.tutorials.mybizz.R

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "bills_reminders_channel"
        const val CHANNEL_NAME = "Bill & Rental Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications for upcoming bills and rental payments"
        private var notificationId = 1000 // Start from 1000
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBillReminder(
        billTitle: String,
        billAmount: Double,
        dueDate: String,
        daysUntilDue: Long,
        billId: String? = null
    ) {
        val title = when (daysUntilDue) {
            0L -> "💰 Bill Due TODAY!"
            1L -> "📅 Bill Due Tomorrow"
            else -> "⏰ Bill Due in $daysUntilDue days"
        }

        val message = "$billTitle - $${"%.2f".format(billAmount)} due on $dueDate"

        showNotification(title, message, billId ?: billTitle)
    }

    fun showRentalReminder(
        tenantName: String,
        property: String,
        rentAmount: Double,
        month: String,
        daysUntilDue: Long,
        rentalId: String? = null
    ) {
        val title = when (daysUntilDue) {
            0L -> "🏠 Rent Due TODAY!"
            1L -> "📅 Rent Due Tomorrow"
            else -> "⏰ Rent Due in $daysUntilDue days"
        }

        val message = "$tenantName - $property - $${"%.2f".format(rentAmount)} for $month"

        showNotification(title, message, rentalId ?: tenantName)
    }

    fun showOverdueReminder(
        title: String,
        message: String,
        itemId: String
    ) {
        showNotification("🚨 OVERDUE: $title", message, itemId + "_overdue")
    }

    private fun showNotification(title: String, message: String, tag: String) {
        val notificationId = notificationId++

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.img_16) // Add this icon to your drawable
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(tag, notificationId, notification)
    }

    fun cancelNotification(tag: String) {
        notificationManager.cancel(tag, 0)
    }
}