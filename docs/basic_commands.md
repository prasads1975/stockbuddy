# StockBuddy — Basic Development Commands

Essential commands for building, installing, testing, and debugging the StockBuddy RFID inventory management app.

## Build Commands

### Clean Build
```bash
./gradlew clean assembleDebug
```
Removes all build artifacts and performs a full clean build of the debug APK.

### Debug Build (Fast)
```bash
./gradlew assembleDebug
```
Builds the debug APK incrementally (faster for iterative development). Includes the
Chainway SDK **ARM native libraries** — this is the build for the **physical C72** device.

### Emulator Build — x86_64 AOSP AVD (`-Pemu`)
```bash
./gradlew assembleDebug -Pemu
```
Same debug APK but with the Chainway **ARM `.so` libraries stripped**. Required to install on a
lightweight **x86_64 AOSP emulator** (which has no ARM translation layer — see the ABI section
below). The app runs fine: it falls back to `EmulatorScannerManager`. **Never flash this build to
a real device** — it has no RFID/imager native libs.

### Release Build
```bash
./gradlew assembleRelease
```
Builds a release APK (requires signing configuration in gradle.properties or local.properties).

### Clean Build with Task Output
```bash
./gradlew clean assembleDebug --info
```
Shows detailed build task output; useful for debugging build failures.

---

## Installation Commands

### Install via Android Studio (Recommended)
```
1. Open Android Studio
2. Press Shift+F10 (or go to Run → Run 'app')
3. Select target emulator or device
```
Uses Android Studio's robust deployment path; more reliable than raw `adb install`.

### Install via ADB (Manual)
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Installs the APK directly to a connected device/emulator. The `-r` flag allows downgrade/reinstall.

### Install and Run
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.gigakin.stockbuddy/.ui.MainActivity
```
Installs the APK and immediately launches the app.

### Known Issue
If you get `Error occurred while checking alignment of package` on emulator, use Android Studio instead of raw `adb install`. This is an emulator-side PackageManager quirk, not a code problem.

---

## Emulator vs Physical Device — Native Libraries (ABI)

The Chainway SDK ships **ARM-only** native libraries (`libDeviceAPIM.so`, `libDeviceAPIQ.so`,
`libIDFingerprintAlg.so`) for `arm64-v8a`, `armeabi-v7a`, and `armeabi`. There is **no x86_64**
variant. This matters when choosing an emulator.

| Target | ABI | Build command | Native libs | Scanner |
|---|---|---|---|---|
| **Physical C72** | arm64-v8a | `./gradlew assembleDebug` | included | real `ChainwayScannerManager` (hardware) |
| **AOSP emulator** | x86_64 | `./gradlew assembleDebug -Pemu` | stripped | falls back to `EmulatorScannerManager` |

### `INSTALL_FAILED_NO_MATCHING_ABIS`
```
adb: failed to install ...: Failure [INSTALL_FAILED_NO_MATCHING_ABIS: ...
    Failed to extract native libraries, res=-113]
```
**Cause**: installing an APK that contains only ARM `.so` files onto an **x86_64** emulator that
has no ARM translation layer (lightweight AOSP images don't include one; Google Play images do).
**Fix**: build the emulator variant that strips the libs — `./gradlew assembleDebug -Pemu` — then
`adb install -r ...`. The `-Pemu` flag is opt-in (`project.hasProperty("emu")` in
`app/build.gradle.kts`); default builds are untouched.

### Verify which APK you have before installing
```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep '\.so$'
```
- **Device build**: shows `lib/arm64-v8a/*.so` (+ armeabi variants) → flash to C72.
- **Emulator build** (`-Pemu`): shows **no** `.so` → install on the x86_64 AOSP AVD.

> Both variants output to the same `app-debug.apk`. Before flashing the C72, run a clean
> **`./gradlew clean assembleDebug`** (no `-Pemu`) so you never push a stripped APK to hardware.

### Recommended emulator: lightweight AOSP AVD
On a low-RAM host, heavy Google Play emulator images (with GMS/Play Services) can trigger the
kernel **low-memory killer**, which `SIGKILL`s the foreground app — appearing as a "crash" with
**no** Java stack trace / `FATAL` in logcat. Prefer an **AOSP** system image (API 34, `x86_64`,
Target "Android 14.0" *without* "Google APIs"/"Google Play"), 3072 MB RAM. It's lighter and
matches the C72 (AOSP, NFR-26). Confirm memory health while the app runs:
```bash
adb shell cat /proc/meminfo | grep -E "MemAvailable|SwapFree"
```
If `SwapFree` is near zero and the app vanishes with no exception, it's an out-of-memory kill, not
a code bug — free host RAM and/or use the AOSP image.

---

## Uninstall Commands

### Uninstall via ADB
```bash
adb uninstall com.gigakin.stockbuddy
```
Completely removes the app from the device/emulator.

### Uninstall (Keep Data)
```bash
adb uninstall com.gigakin.stockbuddy
```
Note: By default, `adb uninstall` does not clear app data.

---

## Logging and Debugging

### View Live Logcat
```bash
adb logcat
```
Streams all system and app logs in real-time.

### Filter Logcat by App Tag
```bash
adb logcat | grep "StockBuddy\|ReportsListFragment\|ResultsFragment"
```
Shows only logs from specific app components (by tag).

### View Only Error and Warning Logs
```bash
adb logcat *:E *:W
```
Filters to show only error and warning level logs.

### Clear Logcat
```bash
adb logcat -c
```
Clears the logcat buffer. Useful before starting a fresh test.

### Save Logcat to File
```bash
adb logcat > ~/stockbuddy_logcat.log
```
Captures all logcat output to a file for later analysis.

### Real-time Filtered Logcat
```bash
adb logcat | grep -i "error\|exception\|crash"
```
Shows only messages containing these keywords (case-insensitive).

---

## Data Management

### Clear App Data (Full Reset)
```bash
adb shell pm clear com.gigakin.stockbuddy
```
Clears all app data, cache, and shared preferences. App launches with clean slate.

### Clear Only Cache
```bash
adb shell rm -rf /data/data/com.gigakin.stockbuddy/cache
```
Clears cache without removing stored data (sessions, configurations).

### Pull Database from Device
```bash
adb pull /data/data/com.gigakin.stockbuddy/databases/
```
Extracts the Room database for inspection on your machine.

### View Shared Preferences
```bash
adb shell cat /data/data/com.gigakin.stockbuddy/shared_prefs/*.xml
```
Displays all SharedPreferences values (`fieldConfigCompleted`, etc.).

---

## Device/Emulator Management

### List Connected Devices
```bash
adb devices -l
```
Shows all connected emulators and physical devices with their states.

### Restart ADB Daemon
```bash
adb kill-server
adb start-server
```
Restarts the ADB service; useful if device connection is stuck.

### Reboot Emulator
```bash
adb reboot
```
Soft-reboots the emulator. Device will disconnect briefly.

### Emulator Wipe and Restart
```bash
emulator -avd Pixel_7 -wipe-data
```
Wipes emulator data and restarts fresh (adjust emulator name as needed; use your AOSP AVD's name).

---

## Gradle Tasks

### List All Available Tasks
```bash
./gradlew tasks
```
Shows all available Gradle tasks (build, test, lint, etc.).

### Run Unit Tests
```bash
./gradlew test
```
Executes all unit tests in the project.

### Run Instrumented Tests (Emulator/Device)
```bash
./gradlew connectedAndroidTest
```
Runs tests on a connected emulator or physical device.

### Check Lint Warnings
```bash
./gradlew lint
```
Analyzes code for potential issues (unused resources, API calls, etc.).

### Build with Verbose Output
```bash
./gradlew assembleDebug --stacktrace
```
Shows full stack trace for build errors.

---

## Development Workflow

### Quick Iteration Cycle
```bash
# 1. Make code changes
# 2. Build
./gradlew assembleDebug

# 3. Install via Android Studio (Shift+F10)

# 4. View logs
adb logcat | grep "StockBuddy"

# 5. If you want a fresh start:
adb shell pm clear com.gigakin.stockbuddy
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.gigakin.stockbuddy/.ui.MainActivity
```

### Troubleshooting Build Failures
```bash
# 1. Clean build
./gradlew clean assembleDebug

# 2. Update dependencies
./gradlew -refresh-dependencies

# 3. If Java version is wrong:
export JAVA_HOME="C:\Users\prasa\.jdks\temurin-17.0.18"

# 4. Check Java version
java -version
```

---

## Environment Setup

### Set JAVA_HOME (Required)
```bash
# On macOS/Linux:
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# On Windows (PowerShell):
$env:JAVA_HOME = "C:\Users\prasa\.jdks\temurin-17.0.18"

# On Windows (Git Bash):
export JAVA_HOME="C:\Users\prasa\.jdks\temurin-17.0.18"
```

### Verify Gradle Wrapper
```bash
./gradlew --version
```
Shows Gradle version and Java installation being used.

---

## Common Issues and Solutions

### Issue: "Error occurred while checking alignment of package"
**Solution**: Use Android Studio's Run button (Shift+F10) instead of `adb install`.

### Issue: App crashes on launch
**Solution**:
```bash
adb logcat -c
adb shell am start -n com.gigakin.stockbuddy/.ui.MainActivity
adb logcat | grep "StockBuddy"
```
This clears logs, launches the app, and shows only relevant errors.

### Issue: Stale data causing test failures
**Solution**:
```bash
adb shell pm clear com.gigakin.stockbuddy
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Fresh install with clean data state.

### Issue: Gradle daemon stuck
**Solution**:
```bash
./gradlew --stop
./gradlew clean assembleDebug
```
Stops all Gradle daemons and restarts fresh.

---

## Quick Reference Cheat Sheet

| Task | Command |
|------|---------|
| Build for **device (C72)** | `./gradlew assembleDebug` |
| Build for **x86_64 AOSP emulator** | `./gradlew assembleDebug -Pemu` |
| Verify APK ABI libs | `unzip -l app/build/outputs/apk/debug/app-debug.apk \| grep '\.so$'` |
| Install (ADB) | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Launch app | `adb shell am start -n com.gigakin.stockbuddy/.ui.MainActivity` |
| View logs | `adb logcat \| grep "StockBuddy"` |
| Clear logs | `adb logcat -c` |
| Clear app data | `adb shell pm clear com.gigakin.stockbuddy` |
| Check memory (OOM debug) | `adb shell cat /proc/meminfo \| grep -E "MemAvailable\|SwapFree"` |
| List devices | `adb devices -l` |
| Uninstall app | `adb uninstall com.gigakin.stockbuddy` |
| Run tests | `./gradlew test` |
| Check lint | `./gradlew lint` |

---

## Notes

- **Package Name**: `com.gigakin.stockbuddy` (used for all adb commands)
- **Main Activity**: `com.gigakin.stockbuddy.ui.MainActivity`
- **Min SDK**: API 28 (Android 9)
- **Target SDK**: API 34
- **Java Version**: 17 (TemurinJDK required)
- **Gradle**: 8.7 (via wrapper in project)

All commands assume you are in the project root directory (`F:\RFIDReaderIntegration\ChainWay\StockBuddy`).
