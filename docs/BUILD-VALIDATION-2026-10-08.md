# Build/Android verification checkpoint — 2026-10-08

## Source
- Base: `COMPROMISSO 0.1.2` security checkpoint.
- Adjustment: removed the redundant `org.jetbrains.kotlin.android` plugin and the `android.builtInKotlin=false` opt-out (Android Gradle Plugin 9.4 uses built-in Kotlin).
- Compose compiler plugin remains enabled.

## Verified in this environment
- **JVM source-test harness:** 13/13 tests passed. This is a standalone Kotlin/JVM runner of actual project test sources, not a Gradle test run.
- **Parser fuzz smoke test:** 40,000 randomized buffers processed without an uncaught exception.
- **Static security gate:** passed (`scripts/security_audit.py`).
- Source archive inspected and extracted.

## NOT verified
- Android Gradle build (`assembleDebug`, `assembleRelease`).
- Android Lint.
- APK merged manifest & actual binary permission audit.
- Android device or emulator tests (`connectedDebugAndroidTest`).
- VPN state, DNS-over-TLS, reboot, Always-on, PIN/Keystore behavior on a physical device.

**Reason:** this execution environment does not have Gradle, Android SDK, ADB, an Android emulator or attached phone. Outbound DNS/network access was also unavailable for installing those tools.

## Windows / Android Studio execution

1. Install Android Studio with Android SDK Platform 37, Build Tools 36.0.0 and Platform-Tools; use JDK 17+.
2. Install Gradle 9.6.0 or generate a Gradle 9.6.0 wrapper for this project. The archive does not currently ship a wrapper executable/jar.
3. Install Python 3 and make it available as `python` in the terminal.
4. Plug in one Android 8.0+ phone via USB, enable Developer options and USB debugging, authorize the PC. Use a *test phone* with no sensitive personal accounts if possible.
5. Open PowerShell at the project root and run:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\build-and-test-windows.ps1
   ```
   The script runs unit tests, Lint, APK build, source and APK permission audits, and Android instrumented tests.
6. Android Studio also supports running `app` on the phone to test the VPN permission prompt, PIN and domain blocklist manually. See `docs/MANUAL_TEST_MATRIX.md`.

**Safety:** Do not enable Device Owner/Lockdown on a personal phone until a full-tunnel VPN and recovery procedure have been separately validated. This release is DNS-first and intentionally prevents activating reinforced lockdown.
