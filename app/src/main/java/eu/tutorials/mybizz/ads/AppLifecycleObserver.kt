package eu.tutorials.mybizz.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class AppLifecycleObserver(
    private val application: Application,
    private val appOpenAdManager: AppOpenAdManager
) : Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null
    private var isAppInForeground = true
    private var isActivityChangingConfigurations = false
    private val showAdDelay = 500L // Delay to avoid showing ad on quick switches

    override fun onActivityStarted(activity: Activity) {
        if (!isActivityChangingConfigurations) {
            if (!isAppInForeground) {
                isAppInForeground = true
                // Show ad when app comes to foreground
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAppInForeground && !isActivityChangingConfigurations) {
                        appOpenAdManager.showAdIfAvailable(activity)
                    }
                }, showAdDelay)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity == currentActivity && !isActivityChangingConfigurations) {
            isAppInForeground = false
            appOpenAdManager.onAppBackgrounded()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
        isActivityChangingConfigurations = activity.isChangingConfigurations
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (activity == currentActivity) {
            currentActivity = null
        }
    }
}