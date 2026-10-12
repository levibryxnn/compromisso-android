package dev.compromisso.app.daily

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyVersePreferencesInstrumentedTest {
    @Test fun preferencesPersistAndCanBeDisabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testPrefs = "daily_verse_instrumented_test_only"
        context.getSharedPreferences(testPrefs, Context.MODE_PRIVATE).edit().clear().commit()
        val first = DailyVersePreferences(context, testPrefs)
        assertFalse(first.enabled)
        assertEquals(8, first.hour)
        first.setTime(7, 45)
        first.enabled = true
        val second = DailyVersePreferences(context, testPrefs)
        assertTrue(second.enabled)
        assertEquals(7, second.hour)
        assertEquals(45, second.minute)
        second.enabled = false
        assertFalse(DailyVersePreferences(context, testPrefs).enabled)
        context.getSharedPreferences(testPrefs, Context.MODE_PRIVATE).edit().clear().commit()
    }
}
