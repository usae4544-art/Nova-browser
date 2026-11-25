# Nova Browser

Privacy-first Android web browser built with Kotlin, Jetpack Compose, and Chromium WebView — styled with an **iPhone iOS 26 Liquid Glass** optical design language, animated **Sakuga VFX** micro-interactions, and built-in security tools.

## Highlights

- **iOS 26 Liquid Glass UI & Optical Shader**
  - Translucent frosted glass surfaces with curved specular meniscus highlights and **Android 13+ AGSL `RuntimeShader`** edge refraction (`RenderEffect.createRuntimeShaderEffect`) over pure glass substrates.
  - **Gliding Liquid Lens Tab Bar & Segmented Controls**: Velocity-stretching spring lens across the Bottom Dock (`Home` ↔ `Tabs`), Tab Switcher (`Tabs` ↔ `Private`), Menu Theme Switcher (`System` | `Dark` | `Light`), and Download filters.
  - **Contextual Scroll Compact Mode**: Top address capsule shrinks gracefully while scrolling long articles while keeping navigation & 3-dot Menu accessible.
  - **28dp Frosted Bokeh Backdrop Blur**: Opens cleanly behind the 3-dot Menu, Tab Switcher, and modals via native Compose blur + hardware `PixelCopy` snapshots for active webpages.
- **Theme-Adaptive Sakuga VFX**
  - Subtle two-tone anime particle accents (`SakugaPalette`) that automatically adapt between **Light Mode**, **Dark Mode**, and **Incognito Vault**, respecting system reduced-motion (`ANIMATOR_DURATION_SCALE`).
- **Privacy, Security & Encrypted DNS**
  - **Local VPN Shield (`NovaVpnService`)**: Encrypted **DNS-over-HTTPS (DoH RFC 8484)** over `VpnService`-protected sockets, **NetShield** DNS ad/tracker blocking (`NXDOMAIN`), Private DNS (`853`) TCP `RST+ACK` fallback, live session telemetry, and Exit IP check.
  - **Ad & Tracker Shield Engine**: Built-in host rules, custom user rules, site allowlists, and HTTPS-Only mode.
  - **Incognito Vault**: `FLAG_SECURE` screen-capture protection, Biometric / Fingerprint unlock (`BiometricPrompt`) + PBKDF2-HMAC-SHA256 PIN fallback, and instant Session Burn data wipe.
- **Productivity & Tools**
  - Multi-tab & Stealth switcher with swipe-blur dismiss, Download Manager with APK Unknown-Sources installer flow + live floating progress card, QR code scanner (CameraX), Reading Mode, Find in Page, Quick Notes, History deduplication, Bookmarks, and Per-site Custom Notification Sounds.

## Tech Stack

- **Language & UI**: Kotlin, Jetpack Compose Material 3, Navigation/Overlays, AGSL `RuntimeShader` (API 33+)
- **Persistence**: Room Database + DataStore Preferences
- **Engine**: Android System WebView (`androidx.webkit`) + Automatic Website Dark Mode CSS injection
- **Camera**: AndroidX CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`)

## Privacy Notice Before Building

- Keystores (`*.keystore`, `*.jks`, `*.p12`), `.env`, `local.properties`, and release artifacts (`*.apk`, `*.aab`) are excluded via `.gitignore`.
- Create or provide your own `debug.keystore` locally for debug builds or configure release keystore environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) in a local untracked environment.

## Building Locally

```bash
./gradlew assembleDebug
```

Pure JVM unit tests:

```bash
./gradlew testDebugUnitTest --tests com.example.NovaBrowserUnitTest --tests com.example.ExampleUnitTest
```
