package dev.compromisso.app.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrustedPinManagerInstrumentedTest {
    @Test
    fun configuresAndVerifiesPinWithKeystoreBackedPepper() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val suffix = System.nanoTime().toString()
        val store = SecurityConfigStore(context, "pin_test_$suffix")
        val manager = TrustedPinManager(store, "compromisso.test.$suffix")

        manager.configure("123456")
        assertTrue(manager.verify("123456") is PinVerification.Success)
        assertTrue(manager.verify("000000") is PinVerification.Invalid)

        context.deleteSharedPreferences("pin_test_$suffix")
    }
}
