package dev.compromisso.app.daily

import android.content.Context

/** Isolated, nonsensitive local settings. No network, browsing or app-list access. */
class DailyVersePreferences(context: Context, prefsName: String = "daily_verse_preferences") {
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) = check(prefs.edit().putBoolean("enabled", value).commit()) {
            "Não foi possível salvar a preferência"
        }

    var hour: Int
        get() = prefs.getInt("hour", 8).coerceIn(0, 23)
        set(value) {
            require(value in 0..23)
            check(prefs.edit().putInt("hour", value).commit())
        }

    var minute: Int
        get() = prefs.getInt("minute", 0).coerceIn(0, 59)
        set(value) {
            require(value in 0..59)
            check(prefs.edit().putInt("minute", value).commit())
        }

    fun setTime(hour: Int, minute: Int) {
        require(hour in 0..23 && minute in 0..59)
        check(prefs.edit().putInt("hour", hour).putInt("minute", minute).commit()) {
            "Não foi possível salvar o horário"
        }
    }
}
