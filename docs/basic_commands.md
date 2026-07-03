# StockBuddy — Basic Development Commands

Essential commands for building, installing, testing, and debugging the StockBuddy RFID inventory management app.

## Build Commands

### Clean Build
```bash
cd app
./gradlew clean assembleDebug
```
Removes all build artifacts and performs a full clean build of the debug APK.

### Debug Build (Fast)
```bash
./gradlew assembleDebug
```
Builds the debug APK incrementally (faster for iterative development).

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
Displays all SharedPreferences values (articleIdMode, fieldConfigCompleted, etc.).

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
emulator -avd Pixel_7_API_34 -wipe-data
```
Wipes emulator data and restarts fresh (adjust emulator name as needed).

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
| Build app | `./gradlew assembleDebug` |
| Install (ADB) | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Launch app | `adb shell am start -n com.gigakin.stockbuddy/.ui.MainActivity` |
| View logs | `adb logcat \| grep "StockBuddy"` |
| Clear logs | `adb logcat -c` |
| Clear app data | `adb shell pm clear com.gigakin.stockbuddy` |
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
