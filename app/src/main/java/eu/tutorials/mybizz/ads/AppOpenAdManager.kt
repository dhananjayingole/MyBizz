package eu.tutorials.mybizz.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdManager(private val application: Application) {
    companion object {
        private const val TAG = "AppOpenAdManager"
        // Replace with your actual Ad Unit ID for App Open Ads
        // For testing, use: ca-app-pub-3940256099942544/9257395921
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

        @Volatile
        private var INSTANCE: AppOpenAdManager? = null

        fun getInstance(application: Application): AppOpenAdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppOpenAdManager(application).also { INSTANCE = it }
            }
        }
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0

    init {
        loadAd()
    }

    private fun loadAd() {
        // Don't load if already loading or if ad is loaded and not expired
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App open ad loaded successfully")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()

                    // Set full screen content callback
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "App open ad dismissed")
                            isShowingAd = false
                            loadAd() // Load next ad
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "App open ad failed to show: ${adError.message}")
                            isShowingAd = false
                            loadAd() // Load next ad
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "App open ad shown")
                            isShowingAd = true
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App open ad failed to load: ${loadAdError.message}")
                    isLoadingAd = false
                    appOpenAd = null
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && !isLoadingAd && !isAdExpired()
    }

    private fun isAdExpired(): Boolean {
        // Ads expire after 4 hours
        val expirationTime = 4 * 60 * 60 * 1000 // 4 hours in milliseconds
        return System.currentTimeMillis() - loadTime >= expirationTime
    }

    fun showAdIfAvailable(activity: Activity, onShowComplete: (() -> Unit)? = null) {
        // Don't show if already showing or if ad is not available
        if (isShowingAd) {
            Log.d(TAG, "App open ad already showing")
            onShowComplete?.invoke()
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "App open ad not available, loading one now")
            loadAd()
            onShowComplete?.invoke()
            return
        }

        appOpenAd?.show(activity)
        onShowComplete?.invoke()
    }

    fun onAppBackgrounded() {
        // Reset when app goes to background
        Log.d(TAG, "App backgrounded")
    }

    fun onAppForegrounded(activity: Activity) {
        // Show ad when app comes to foreground
        Log.d(TAG, "App foregrounded, showing app open ad")
        showAdIfAvailable(activity)
    }
}