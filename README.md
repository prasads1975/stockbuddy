# StockBuddy — MVP Build

Android project for the RFID Inventory MVP/Demo scope (`com.gigakin.stockbuddy`).
Generated against SRS v3.14 and the companion MVP/Demo Scope doc — comments throughout
the source reference specific FR/NFR IDs so you can trace any piece of code back to its
requirement.

## Before you open this in Android Studio

1. **Gradle wrapper jar is missing.** I couldn't download it in the sandbox this was
   generated in (no network access there). Either:
   - Open the project in Android Studio — it will offer to regenerate the wrapper, or
   - Run `gradle wrapper --gradle-version 8.7` once you have Gradle installed locally, or
   - Just use your local Gradle/Android Studio's bundled Gradle to sync directly.
2. **Chainway SDK is not included.** Drop the SDK `.aar` (and any supporting files) into
   `app/libs/` (see `app/libs/README.md`), then uncomment the dependency line in
   `app/build.gradle.kts`. Until then, the app automatically falls back to
   `EmulatorScannerManager` (see `hardware/ScannerManagerProvider.kt`) so it builds and
   runs fine without the SDK — see NFR-26a / FR-81a.
3. **Branding/logo** is a placeholder (`drawable/ic_launcher_foreground.xml`) — swap when
   the real assets arrive (Section 7, MVP Scope doc).

## What's implemented (MVP scope)

- Domain Field Configuration with starter templates + Article ID toggle (FR-77i-o)
- Category Management (FR-72/73)
- Individual Linking — manual entry, dynamic fields, Barcode/RFID scan buttons (FR-05-10, NFR-10f)
- Bulk Linking — CSV import (FR-15-19)
- Inventory sessions — START/STOP, category filter, real-time dedup (FR-29-37)
- Results — Available/Missing tabs, grouped by Barcode (FR-40-47)
- Export — Share + Download (FR-49-52a)
- Assets — search, category filter (FR-61-67)
- Hardware connection status bar + graceful emulator degradation (FR-81/81a, NFR-26a)
- Demo Mode Limits — 50 items / 10 categories / 25 sessions (rolling retention), all sourced
  from `BuildConfig` (see `app/build.gradle.kts` and `util/DemoLimits.kt`)

## What's NOT implemented (explicitly out of MVP scope)

QR Scan linking mode, Product Master screen, API/LAN export channels, login/RBAC,
Licensing, Backup & Restore, the non-skippable first-run wizard, Excess tab. See Section 2
of the MVP/Demo Scope doc for the full list and rationale.

## Architecture

Layered per NFR-27: `ui/` (Fragment + ViewModel, ViewBinding) → `data/repo/` (business logic,
validation, demo-limit enforcement) → `data/db/` (Room/SQLite). Hardware access is fully
abstracted behind `hardware/ScannerManager` — nothing outside that package talks to the
Chainway SDK directly, which is what makes the emulator fallback (NFR-26a) a one-place concern.

Manual dependency wiring lives in `StockBuddyApp.kt` (no DI framework — see Section 7 of the
build notes for why, and as a candidate to revisit if the codebase grows).

## Known gaps / next steps for a developer picking this up

- Field-level validation error display for dynamic fields is best-effort (matches by key on
  the `TextInputLayout` parent) — fine for MVP, but a custom view binder would be cleaner.
- Excess-tab UI isn't wired into the Results tabs (computation already happens in
  `InventoryRepository.computeResults` — it's a one-tab addition away).
- `ExportRepository`'s "Download" (FR-52a) currently saves to the app's external-files
  directory rather than a full Storage Access Framework location picker — functionally
  correct for MVP, upgradeable later without changing the repository's public contract.
- Test data: see `StockBuddy_Test_Values_RFID_Barcode.md` (separate doc) for sample
  RFID/Barcode values to type into the emulator manually.
