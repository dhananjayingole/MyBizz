package eu.tutorials.mybizz

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.firebase.FirebaseApp
import eu.tutorials.mybizz.Notification.ReminderScheduler
import eu.tutorials.mybizz.ads.AppLifecycleObserver
import eu.tutorials.mybizz.ads.AppOpenAdManager
import eu.tutorials.mybizz.language.LocaleManager

class MyBizApp : Application() {
    companion object {
        lateinit var appContext: Context
            private set
        lateinit var appOpenAdManager: AppOpenAdManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) { initializationStatus: InitializationStatus ->
            // AdM8 initialized successfully
        }

        // Initialize App Open Ad Manager
        appOpenAdManager = AppOpenAdManager.getInstance(this)

        // Register lifecycle callbacks
        registerActivityLifecycleCallbacks(
            AppLifecycleObserver(this, appOpenAdManager)
        )

        // Schedule daily reminders when app starts
        val scheduler = ReminderScheduler(this)
        scheduler.scheduleDailyReminders()
    }
}