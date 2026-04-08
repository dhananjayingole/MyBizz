package eu.tutorials.mybizz.language

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {

    // ── Supported languages ────────────────────────────────────────────────
    /** Map of language code → display name shown in the picker UI */
    val supportedLanguages: Map<String, String> = linkedMapOf(
        "en" to "English",
        "hi" to "हिंदी",
        "mr" to "मराठी"
    )

    // ── SharedPreferences key ──────────────────────────────────────────────
    private const val PREF_FILE = "app_language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val DEFAULT_LANGUAGE = "en"

    // ── Public API ─────────────────────────────────────────────────────────

    /** Returns the currently saved language code (e.g. "en", "hi", "mr"). */
    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * Persists the chosen language code and returns a Context with the new
     * locale applied, ready to be passed to super.attachBaseContext().
     */
    fun setLocale(context: Context, languageCode: String): Context {
        saveLanguagePreference(context, languageCode)
        return applyLocale(context, languageCode)
    }

    /**
     * Reads the persisted language and applies it to the given context.
     * Call this from Activity.attachBaseContext().
     */
    fun applyLocale(context: Context): Context {
        val language = getSelectedLanguage(context)
        return applyLocale(context, language)
    }

    /**
     * Restarts the given Activity so the new locale takes effect everywhere.
     * Typically called right after setLocale().
     */
    fun restartActivity(activity: Activity) {
        val intent = activity.intent ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        activity.startActivity(intent)
        activity.finish()
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun saveLanguagePreference(context: Context, languageCode: String) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    private fun applyLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}