# RF//RECON — Passive RF Intelligence for Android

Native Kotlin/Jetpack Compose starter project for passive Wi-Fi and BLE awareness.

## Current implementation
- Android Wi-Fi scan-result collection (uses OS-provided scan results; requests a scan when started).
- BLE advertisement scanning in low-power mode; no GATT connection or active probing.
- Basic SSID/BSSID/RSSI/frequency/channel/security parsing.
- BLE address/name/RSSI/Tx power/manufacturer data/service UUID extraction.
- Session display and basic radar visualization.
- SQLite persistence for Wi-Fi and BLE observations.
- HTML report and CSV/PDF export helper functions.

## Build
Open this folder in Android Studio (JDK 17, Android SDK 35), allow Gradle sync, then run on a physical Android device.

## Android permissions and platform constraints
- Grant Nearby devices / Bluetooth scan permissions and precise location as requested.
- Android Wi-Fi scan throttling applies. `startScan()` can be rate-limited or return false; scan results may be cached.
- Wi-Fi scan-result access is subject to Android version, location permission, and location-services policy.
- BLE scan behavior and background execution are subject to Android OS and OEM restrictions.
- This starter does not yet implement a foreground service, persistent route recorder lifecycle, map tile rendering, heatmap aggregation, or SAF export screens. The map-ready GPS points and export helpers are foundations, not a completed mapping/report workflow.
- Android may randomize BLE addresses. Treat addresses as observed identifiers, not guaranteed physical-device identities.
- A reported RSSI is not a reliable distance measurement.

## Next engineering steps
1. Add a foreground service with explicit user-visible session controls and robust lifecycle handling.
2. Persist GPS points and attach location to each observation at observation time.
3. Add offline/online map UI and heatmap aggregation.
4. Add Storage Access Framework screens for HTML/CSV/PDF exports.
5. Add OUI and assigned-number datasets with versioning and provenance.
6. Add security/anomaly rules with confidence and evidence fields.
7. Add Room migrations, retention controls, tests, and device/API compatibility testing.

## Design
Dark graphite surfaces, terminal typography, neon green/cyan telemetry accents, and compact operator-console layouts.
