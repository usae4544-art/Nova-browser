## Nova Browser v1.0.4

### What's New

- **🔐 Persistent App Signing Key for Clean OTA Updates**: Switched GitHub Actions release signing to a permanent keystore secret (`NOVA_DEBUG_KEYSTORE_BASE64`) so every update from `v1.0.4` onward (`v1.0.5`, `v1.0.6`, ...) upgrades cleanly in-place without package signature conflicts.
- **🌐 Sign in with Google (Active Across All Websites) + Full Account Creation**:
  - Added **Continue with Google** & **Create Google Account** (`accounts.google.com`) with Chrome-standard User-Agent & flushed Chromium auth cookies so your Google account stays logged in across YouTube, Gmail, and **"Continue with Google"** buttons on websites.
  - Upgraded Nova Account dialog with **Sign In** and **Create Account** tabs, salted SHA-256 passwords, saved-account switching, and live Google connection status.
- **☀️ Balanced Soft Daylight Light Mode & Dark Text Contrast Fix**:
  - Replaced overly bright pure-white glare in Light Mode with calm cool slate-mist Liquid Glass surfaces, toned down AGSL shader highlights, moved specular sheen behind foreground text, and set theme `LocalContentColor` so all cards and download banners stay sharp.
- **👇 Improved 3-Dot Menu Top Tap Space & Pull-Down Notch Animation**:
  - Reserved comfortable touchable space above the 3-dot menu sheet so tapping the top backdrop cleanly dismisses back.
  - Added smooth vertical pull-down drag gestures with live follow, spring bounce-back, and animated dismissal on the top notch/handle.
