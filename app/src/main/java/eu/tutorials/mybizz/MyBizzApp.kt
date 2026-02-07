package eu.tutorials.mybizz

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import eu.tutorials.mybizz.Notification.ReminderScheduler

class MyBizApp : Application() {
    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Schedule daily reminders when app starts
        val scheduler = ReminderScheduler(this)
        scheduler.scheduleDailyReminders()
    }
}