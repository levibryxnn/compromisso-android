package dev.compromisso.app.ui

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.compromisso.app.MainActivity
import dev.compromisso.app.security.SecurityConfigStore
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates UI snapshots from the real Compose activity on a clean emulator.
 *
 * Security note: FLAG_SECURE is cleared only on this temporary test Activity
 * instance so instrumentation can capture the pixels. The shipping/debug
 * application retains FLAG_SECURE. No PIN, browser data, or personal files
 * are captured.
 */
@RunWith(AndroidJUnit4::class)
class UiPreviewInstrumentedTest {
    @Test
    fun captureActualHomeScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        SecurityConfigStore(context).onboardingComplete = true

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(1500)

            val screenshot = instrumentation.uiAutomation.takeScreenshot()
                ?: error("Could not capture rendered Compose UI")
            val previewDir = File(
                requireNotNull(context.getExternalFilesDir(null)) { "No app-private preview directory" },
                "previews",
            )
            assertTrue(previewDir.mkdirs() || previewDir.isDirectory)
            val file = File(previewDir, "compromisso-home.png")
            file.outputStream().use { stream ->
                check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
            screenshot.recycle()
            assertTrue(file.isFile && file.length() > 1000)

            // connectedDebugAndroidTest may uninstall the application after testing,
            // deleting /sdcard/Android/data/... before the runner can pull it.
            // The test-only UI automation shell writes an independent emulator PNG.
            // This does not grant storage permissions to the application APK.
            val exported = "/sdcard/Download/compromisso-home.png"
            val copyResult = instrumentation.uiAutomation.executeShellCommand(
                "sh -c 'screencap -p $exported && wc -c < $exported'"
            )
            val output = android.os.ParcelFileDescriptor.AutoCloseInputStream(copyResult)
                .bufferedReader()
                .use { it.readText().trim() }
            check((output.toLongOrNull() ?: 0) > 1000L) {
                "Preview export failed: $output"
            }
        }
    }
}
