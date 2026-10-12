# Security hardening: COMPROMISSO 0.1.1 (source review)

Status: **source-level hardening only — NOT release-approved**.

## Security boundary and privacy

This application may inspect DNS query domain names through an Android user-approved
`VpnService` to enforce the locally configured list. It does not read app files,
photos, media, messages, bank-account information, app inventories, microphone,
camera, contacts or location. No Accessibility Service, external storage access,
tracking SDK, analytics or advertising SDK is included.

The app queries the external public DNS provider **Cloudflare** (`1.1.1.1:853`,
`cloudflare-dns.com`) to resolve permitted DNS names. This does not store
history in the app, but Cloudflare receives those domain queries and some
network metadata. The privacy model must disclose this dependency.

## Changes applied

1. `android:allowBackup=false`, legacy full-backup exclusions and Android 12+
   cloud and device-to-device extraction exclusion rules.
2. `android:usesCleartextTraffic=false` and removal of UDP/53 upstream resolver.
3. DNS-over-TLS with system TLS trust, hostname verification, protected socket,
   fixed IP bootstrap, timeouts, DNS ID/response validation, and no plaintext fallback.
   Resolver failure returns `SERVFAIL` for otherwise allowed DNS queries.
4. Activity `FLAG_SECURE`, plus Android 12+ `HIDE_OVERLAY_WINDOWS` to reduce
   screen capture and tapjacking on sensitive views.
5. Local Compose digit pad for PIN entry; no third-party IME input of the PIN.
6. AES-GCM blocklist domain and category encryption with non-exportable
   Android Keystore key. HMAC-SHA256 fingerprint supports uniqueness without
   plaintext lookups. SQLite schema v1→v2 migration encrypts existing rules.
   `PRAGMA secure_delete=ON` minimizes SQLite freed-page retention but cannot
   guarantee secure erasure of data from flash or old backups.
7. PIN attempt-count and lock deadline are committed atomically.
8. VPN worker shutdown now clears the runtime green status.
9. Automated source security gate enforces permission allowlist, minimum
   manifest restrictions and rejects some dangerous APIs/regressions.

## What is NOT guaranteed or implemented

- **Not a full traffic firewall:** VPN routes IPv4 DNS toward the app's
  private DNS endpoint; DoH/DoT originating inside browsers and other apps,
  direct IP addresses, IPv6 and custom proxies remain possible bypasses.
- The green toggle confirms a running DNS-only VPN interface, **not** full
  protection or immunity against malicious Android apps.
- `usesCleartextTraffic=false` does not automatically prohibit all raw socket
  traffic. The DNS implementation itself uses TLS. Non-DNS app traffic is not
  intercepted.
- `FLAG_SECURE` and hiding overlays cannot defend against a compromised OS,
  privileged accessibility malware, physical cameras, root or USB debugging.
- Device Owner restrictions for `DISALLOW_APPS_CONTROL` / `DISALLOW_ADD_USER`
  were **not** enabled: those policies affect the device or other applications,
  exceed the narrow least-privilege scope, and need informed product decisions.
- The default Android app cannot make itself impossible to force-stop, clear
  data, or uninstall. Android user/device administrative authority prevails.
- Encryption at rest relies on Android sandbox + Keystore. No absolute claims
  about root, forensic extraction, device compromise or vendor implementation.
- URL keyword matching across HTTPS content is **not** implemented. Adding
  invasive Accessibility monitoring or TLS interception would violate the
  current privacy goals without a separate explicit design review.

## Validation status

- Source security audit: PASS for declared allowlist and banned API string scan.
- Kotlin smoke test: PASS for domain matching, DNS validation/error packet,
  DoT message framing and local PIN rate-limit policy.
- XML parsing: PASS.
- Android Gradle build: NOT RUN (Android SDK and Gradle not installed locally).
- Android instrumentation: NOT RUN; `EncryptedDomainInstrumentedTest` includes
  read/write encryption, uniqueness and v1→v2 migration coverage.
- APK merged-manifest/dependency audit: NOT RUN, required before release.
- Real-device / browser bypass matrix: NOT RUN, required before release.

## Next highest-priority work

1. Install Android toolchain or publish to CI; run `testDebugUnitTest`,
   `lintDebug`, `assembleDebug`, Android instrumented tests and dependency audit.
2. Test restoring/migrating existing users, no data loss, PIN flows, DNS provider
   outages and captive portals on Android 8.0 through current Android.
3. Inspect merged manifest and APK permissions; fail CI on unwanted changes.
4. Build a privacy-preserving true full-tunnel/filtering architecture, including
   IPv6 and application DoH bypass protections, before anti-bypass marketing.
5. Review reinforced Device Owner mode separately and preserve user recovery
   and legitimate device control.
