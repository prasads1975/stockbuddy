# StockBuddy — CLAUDE.md

## What this project is

Android RFID Inventory Management app for the Chainway C72 handheld scanner.
Package: `com.gigakin.stockbuddy` | SRS: `RFID_App_SRS_C72.md` v3.14 | Scope: `StockBuddy_MVP_Demo_Scope.md`

The core sales pitch: **one APK, reconfigured in 30 seconds for any retail industry** (toy store, jewellery, pharma, etc.) by switching domain-specific field configuration at runtime — no code changes, no APK update. This is the headline feature. Every architecture decision serves it.

Current phase: **MVP/Demo only.** The app must build, run on an Android emulator, and support a live demo walkthrough without a physical C72 scanner present.

---

## Hard constraints — read before touching any file

**Never do these:**
- Add Google Play Services or any GMS dependency. The C72 runs AOSP (NFR-26). Check every new library.
- Add Hilt, Dagger, or any DI framework. Manual wiring in `StockBuddyApp.kt` is intentional.
- Use Article ID as a uniqueness key, grouping key, or Product Master matching key. Only `rfidTagId` (uniqueness) and `barcode` (grouping/matching) have those roles. Article ID is always nullable supplementary data.
- Add internet permissions or outbound API calls. The app is fully offline in MVP (NFR-09, NFR-15).
- Use `Bundle` directly for navigation arguments. Always use Safe Args typed Directions/Args classes.
- Implement licensing, authentication/RBAC, backup/restore, or the first-run wizard. These are explicitly out of scope.
- Implement QR scan mode (FR-01–04). Cut from MVP.
- Change `viewBinding = false` anywhere. ViewBinding is the codebase-wide pattern.
- Simplify or remove the hybrid fixed-columns + JSON-attributes data model. It is the architectural foundation of the dynamic-field feature — see Data Model section below.
- Change demo limits without explicit instruction. They are `BuildConfig` constants by design.

---

## Technology stack (all decisions are final for this phase)

| | Decision |
|---|---|
| **Language** | Kotlin (primary) + Java (Chainway SDK integration only) |
| **UI** | XML layouts + ViewBinding. Not Compose. |
| **Design system** | Material Design 3. `Theme.Material3.DayNight.NoActionBar`. |
| **Architecture** | MVVM: Fragment → ViewModel → Repository → Room DAO |
| **DI** | Manual service locator in `StockBuddyApp.kt`. No framework. |
| **Navigation** | Jetpack Navigation Component + Safe Args plugin |
| **Database** | Room v1 + SQLite. Local-only. No migrations yet. |
| **Hardware** | Interface + 2 implementations (see Hardware section) |
| **CSV** | OpenCSV 5.9 |
| **Coroutines** | `viewModelScope` + suspend functions throughout |
| **Min SDK** | API 28 (Android 9) |
| **Build** | Gradle Kotlin DSL (`build.gradle.kts`) |

---

## Project structure

```
app/src/main/java/com/gigakin/stockbuddy/
├── StockBuddyApp.kt              ← Application class; all repositories wired here
├── hardware/                     ← ONLY place Chainway SDK may be called
│   ├── ScannerManager.kt         ← Interface + RfidScanResult sealed class
│   ├── EmulatorScannerManager.kt ← Always NOT_AVAILABLE; used on emulator
│   ├── ChainwayScannerManager.kt ← SDK TODOs; currently returns ReaderUnavailable
│   └── ScannerManagerProvider.kt ← Picks real vs emulator; catches init failures
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt        ← Room singleton, version 1
│   │   ├── entity/               ← 6 entities (see Data Model)
│   │   └── dao/                  ← 6 DAOs
│   ├── prefs/AppPrefs.kt         ← SharedPrefs: articleIdMode + fieldConfigCompleted
│   └── repo/                     ← 6 repositories (all business logic lives here)
├── util/
│   ├── FieldType.kt              ← Enums: FieldType, ArticleIdMode, ReaderStatus, LimitCheck
│   ├── DemoLimits.kt             ← BuildConfig constants: MAX_ITEMS=50, CATEGORIES=10, SESSIONS=25
│   ├── JsonAttributes.kt         ← toMap() / fromMap() for the JSON attributes column
│   ├── CsvUtils.kt               ← OpenCSV wrapper
│   └── ViewModelFactory.kt       ← Generic lambda factory
└── ui/
    ├── MainActivity.kt           ← Single activity; NavHostFragment; global reader status bar
    ├── home/                     ← HomeFragment (4-tile hub)
    ├── linking/                  ← LinkingOptions, IndividualLinking*, BulkLinking*
    ├── inventory/                ← InventoryEntry, Scanning*, ReportsList*
    ├── results/                  ← Results*, ResultGroupAdapter
    ├── export/                   ← ExportBottomSheetFragment
    ├── assets/                   ← Assets*
    ├── category/                 ← Category*
    ├── fieldconfig/              ← FieldConfig*, FieldDefAdapter
    └── settings/                 ← SettingsFragment

app/src/main/res/
├── navigation/nav_graph.xml      ← All 12 fragments + all navigation actions
├── layout/                       ← 20 layout XMLs
├── values/colors.xml             ← MD3 palette + semantic colours
├── values/themes.xml             ← MD3 theme + 3 custom styles
├── values/strings.xml            ← All UI strings
└── drawable/                     ← 12 vector icons
```

---

## Data model (critical — read carefully)

### The hybrid design

Every item record has two kinds of fields:

**Fixed columns** (Room DB columns, always present):
- `rfidTagId: String` — `@PrimaryKey` on `items`. The guaranteed-unique per-unit identifier. NFR-18.
- `barcode: String` — `@PrimaryKey` on `products`. The product-level grouping and matching key.
- `name: String`, `categoryName: String`
- `articleId: String?` — nullable; presence controlled by `ArticleIdMode` in `AppPrefs`

**Domain-specific JSON column** (the dynamic-field feature):
- `attributesJson: String` — single JSON blob per item. Schema defined at runtime in `field_definitions` table. Read/write via `JsonAttributes.toMap()` / `JsonAttributes.fromMap()`.

### Entities and tables

```
categories          CategoryEntity       PK: id (autoGen), UNIQUE: name
field_definitions   FieldDefinitionEntity PK: id (autoGen) — field schema, sortOrder
products            ProductEntity        PK: barcode — auto-upserted on every item save
items               ItemEntity           PK: rfidTagId — one row per physical tagged unit
inventory_sessions  InventorySessionEntity PK: id (autoGen), code, createdAt, stoppedAt?
scanned_tags        ScannedTagEntity     PK: id, FK: sessionId→sessions CASCADE, UNIQUE(sessionId,rfidTagId)
```

### Key rules baked into the data layer

- RFID uniqueness → DB-level (`@PrimaryKey`) + repo-level check before insert → returns `SaveResult.DuplicateRfid`
- Scan dedup → `OnConflictStrategy.IGNORE` on `ScannedTagDao.insert()` — no app-level dedup needed
- Product Master upsert → `OnConflictStrategy.REPLACE` keyed on `barcode`, called automatically after every item save
- Session rolling purge → `InventoryRepository.startSession()` deletes the oldest session when count ≥ 25, then inserts
- Demo item cap → checked in `ItemRepository.saveLinkedItem()` and `bulkImport()` before insert

---

## Article ID mode rules

Article ID behaviour is controlled by `AppPrefs.articleIdMode: ArticleIdMode` (NOT_USED / OPTIONAL / MANDATORY, default OPTIONAL).

| Where enforced | What happens |
|---|---|
| `IndividualLinkingFragment.onViewCreated()` | Sets `layoutArticleId.visibility` based on mode |
| `ItemRepository.saveLinkedItem()` | Validates non-blank only when mode is MANDATORY |
| `ItemRepository.bulkImport()` | Reads Article ID CSV column only when mode != NOT_USED |
| `ExportRepository.buildCsv()` | Includes Article ID column only when `articleIdUsed = true` |
| `AssetsAdapter`, `ResultGroupAdapter` | Shows Article ID only when `articleIdEnabled` constructor param is true |
| `FieldConfigFragment` | Radio group maps to the three modes |

---

## Hardware abstraction rules

The `hardware/` package is the **only** place Chainway SDK classes may be referenced.

### `ScannerManager` interface
```kotlin
val status: LiveData<ReaderStatus>         // CONNECTED | NOT_CONNECTED | NOT_AVAILABLE
suspend fun scanSingleRfidTag(): RfidScanResult
fun startContinuousScan(onTagRead: (String) -> Unit)
fun stopContinuousScan()
suspend fun scanBarcode(): String?
```

### `RfidScanResult` sealed class
```kotlin
Success(epc: String) | NoTagDetected | MultipleTagsDetected(count: Int) | ReaderUnavailable
```

### FR-81a rule (enforced in Fragments)
When `status == NOT_AVAILABLE`, all scan-triggering buttons must be **disabled** with a clear inline message. The emulator manager returns `ReaderUnavailable` as a safety net, but disabling the button is the primary safeguard.

### Wiring the real SDK (when `app/libs/chainway-sdk.aar` is present)
1. Uncomment `implementation(name = "chainway-sdk", ext = "aar")` in `app/build.gradle.kts`
2. Add required `<uses-permission>` entries to `AndroidManifest.xml` (check SDK docs)
3. Fill in `TODO` blocks in `ChainwayScannerManager.kt`: `init{}`, `scanSingleRfidTag()`, `startContinuousScan()`, `stopContinuousScan()`, `scanBarcode()`
4. Update `ScannerManagerProvider.create()` to detect SDK absence explicitly

---

## UI/UX rules (enforced on every screen)

| Rule | NFR | Requirement |
|---|---|---|
| Design system | NFR-10a | MD3 components only |
| Form factor | NFR-10b | Design for 6-inch portrait display (C72's screen) |
| Primary action size | NFR-10c | One primary action per screen, min 56dp height, full/near-full width |
| Thumb zone | NFR-10d | Primary action pinned at the bottom, outside scroll area |
| Screen density | NFR-10e | One primary action + minimum info per screen; defer everything else |
| Scan button hierarchy | NFR-10f | Barcode = compact end-icon on TextInputLayout. RFID = full-width labelled button. Save Link = the single primary action at bottom. These three are visually distinct. |
| Colour coding | NFR-11 | Available=`#1B873D` green, Missing=`#BA1A1A` red, Excess=`#B8860B` amber. Always paired with icon/text — never colour alone. |
| Validation errors | NFR-13 | Inline on the field via `TextInputLayout.error`. Plain language. No stack traces. |
| Feedback visibility | NFR-13a | Snackbars for transient confirmations. Critical/save-blocking errors persist until resolved. Not obscured by keyboard or thumb. |
| Reader status bar | FR-81 | Persistent `TextView` in `activity_main.xml` above NavHostFragment. Always visible. Three states: Connected (green), Not Connected (amber), Not Available (grey). |

### Custom styles (in `res/values/themes.xml`)
- `@style/Widget.StockBuddy.PrimaryActionButton` — all primary actions (56dp min height)
- `@style/Widget.StockBuddy.RfidScanButton` — RFID scan button specifically
- `@style/Widget.StockBuddy.BarcodeScanIcon` — inline icon buttons (48dp touch target)

---

## Repository business logic summary

### `ItemRepository.saveLinkedItem()` — validation order
1. Fixed mandatory fields blank check (name, barcode, category, rfidTagId)
2. Article ID mode-dependent validation (required only when MANDATORY)
3. Domain-specific mandatory fields (from `field_definitions` where `mandatory = true`)
4. RFID duplicate check → `SaveResult.DuplicateRfid`
5. Demo cap check → `SaveResult.DemoLimitReached`
6. DB insert + Product Master upsert (always called on success)

### `ItemRepository.SaveResult` sealed class
```kotlin
Success | ValidationError(fieldErrors: Map<String, String>) | DuplicateRfid | DemoLimitReached
```
Field error keys: `"name"`, `"barcode"`, `"category"`, `"rfid"`, `"articleId"`, `"attr_{fieldKey}"`

### `InventoryRepository.computeResults(sessionId, categoryFilter?)`
- Available = scanned AND in item master (within filter scope)
- Missing = in item master but NOT scanned (within filter scope)
- Excess = scanned but NOT in item master → synthetic `ItemEntity(name="(Unrecognized tag)")`
- Filter is applied to the master set at compute time — raw scanned_tags is never filtered or discarded (FR-46)

### `ExportRepository.buildCsv()` — column order
`Status | Name | Barcode | Category | [ArticleId if enabled] | [domain fields in sortOrder] | RFID Tag ID`

---

## Navigation map

```
Home ──► LinkingOptions ──► IndividualLinking
                        └──► BulkLinking

Home ──► InventoryEntry ──► Scanning ──► Results (popUpTo: Home)
                        └──► ReportsList ──► Results

Home ──► Assets

Home ──► Settings ──► Category
                  └──► FieldConfig

Results ──► [ExportBottomSheet — dialog, not in nav graph]
             shown via: parentFragmentManager.show()
```

Safe Args typed classes generated at build time:
`HomeFragmentDirections`, `LinkingOptionsFragmentDirections`, `ScanningFragmentArgs`, `ScanningFragmentDirections`, `ReportsListFragmentDirections`, `ResultsFragmentArgs`, `SettingsFragmentDirections`

---

## Demo mode limits

Defined as `BuildConfig` fields in `app/build.gradle.kts`. Read via `DemoLimits.kt`. Enforced in repositories, not UI.

| Resource | Limit | Enforcement | At-limit behaviour |
|---|---|---|---|
| Items | 50 | `ItemRepository` | Block save; return `DemoLimitReached`; bulk import skips remaining rows |
| Categories | 10 | `CategoryRepository` | Block add; return `LimitCheck.Exceeded(message)` |
| Sessions | 25 | `InventoryRepository` | Rolling purge — delete oldest, then insert |

---

## Current state — what is and isn't done

### Done (all files exist and contain real logic)
- Complete Room data layer: 6 entities, 6 DAOs, `AppDatabase`
- All 6 repositories with full validation and demo-limit enforcement
- Hardware abstraction layer (emulator + SDK placeholder)
- All 12 screens: Home, LinkingOptions, IndividualLinking, BulkLinking, InventoryEntry, Scanning, ReportsList, Results, ExportBottomSheet, Assets, Category, FieldConfig, Settings
- `nav_graph.xml` with all fragments, actions, and Safe Args typed arguments
- MD3 theme, colors, strings, 20 layout XMLs, 12 vector drawables
- `StockBuddyApp.kt` wiring all repositories
- `AndroidManifest.xml` with FileProvider for CSV sharing

### Known gaps — fix in this order

**P0: Build errors** ✅ RESOLVED
- ✅ Gradle wrapper JAR regenerated (8.7)
- ✅ Safe Args classes generated correctly
- ✅ Missing import added to ResultsFragment
- ✅ XML namespace fixed (res-auto)
- ✅ Application ID corrected (no suffix)
- ✅ Room exportSchema issue resolved

**P1: Functional gaps** ✅ RESOLVED
1. ✅ **DROPDOWN field rendering** — `IndividualLinkingFragment.renderDynamicFields()` now creates `ExposedDropdownMenu` style `TextInputLayout` with `AutoCompleteTextView` for DROPDOWN type fields, populated from `dropdownOptionsCsv`.
2. ✅ **Excess tab in Results** — `ResultsFragment` now displays three tabs (Available, Missing, Excess). Tab-switching logic updated to handle all three statuses.
3. ✅ **Dynamic field error display verified safe** — Cast chain `dynamicFieldViews[fieldKey]?.parent as? TextInputLayout` uses proper null-safe operations throughout. No crash risk.

**P2: Polish** ✅ RESOLVED
4. ✅ **Hardcoded strings extracted** — `"(Unrecognized tag)"` moved to `strings.xml` resource file.
5. ✅ **RecyclerView dividers added** — `DividerItemDecoration` applied to both Assets and Results lists for improved visual separation.
6. ✅ **Home tile sizing improved** — Converted `GridLayout` to `ConstraintLayout` with horizontal chains for predictable, equal-width tiles on all screen sizes (especially reliable on 6-inch C72 display).

---

## Project Setup & Infrastructure

### Dependency Status ✅
- **All required libraries present**: AndroidX, Material Design 3, Navigation, Room, Coroutines, OpenCSV, JSON
- **No Google Play Services**: NFR-26 fully satisfied (AOSP-safe)
- **Gradle**: 8.7 configured, wrapper present, flatDir ready for SDK
- **Java/Kotlin**: 17 target, 1.9.24 Kotlin compiler, KSP annotation processing working

### Chainway SDK Status ✅ COMPLETE
- **Status**: SDK fully integrated (DeviceAPI_ver20251103_release.aar)
- **Build status**: ✅ BUILD SUCCESSFUL with SDK included
- **Native libraries**: 
  - libDeviceAPIM.so (UHF RFID module)
  - libDeviceAPIQ.so (Imager module)
  - libIDFingerprintAlg.so (ID fingerprint algorithm)
- **Implementation**: All TODO blocks completed using actual Chainway SDK APIs:
  - ✅ `init()` — RFIDWithUHFUART.getInstance() + init(context: Context)
  - ✅ `scanSingleRfidTag()` — inventorySingleTag() with TX power management (FR-07a)
  - ✅ `startContinuousScan()` — startInventoryTag() + setInventoryCallback()
  - ✅ `stopContinuousScan()` — stopInventory()
  - ⏳ `scanBarcode()` — placeholder (imager API confirmation pending)
  - ✅ Power control: setPower(18) for short-range, setPower(27) for continuous
  - ✅ Graceful fallback to EmulatorScannerManager if SDK init fails
- **Ready for**: Testing on physical Chainway C72 device

### Git Configuration ✅ COMPLETE
- **Status**: Git repository initialized with 5 commits
- **Current branch**: master
- **Commits**:
  - df95e10: Initial commit — MVP complete (P0/P1/P2)
  - 757a4fc: Wire Chainway SDK integration
  - 818ee87: Update documentation for SDK integration
  - 49f98e6: Implement reflection-based SDK integration (first pass)
  - c7dbda8: Replace with actual Chainway SDK API
- **Setup**: `.gitignore` configured; all source code tracked
- **Ready for**: Team collaboration, CI/CD setup, deployment

## Explicitly out of MVP scope — do not implement

- QR Scan linking mode (FR-01–04, FR-11)
- Product Master screen (FR-22–28) — auto-upsert handles it without a UI
- API upload / LAN transfer export (FR-53–56)
- Authentication / RBAC / login screen
- Licensing screen
- Backup & Restore
- Non-skippable First-Run Wizard (FR-77–79)
- Google Play Services of any kind

---

## First actions when starting a session

```bash
# 1. Ensure the build works
cd StockBuddy/
./gradlew clean assembleDebug    # full clean build

# 2. Run on emulator — use Android Studio, NOT adb install
#    Android Studio → Run (Shift+F10) → select emulator
#    This uses a more robust deployment path than raw adb.
#
#    If you must use adb, the command is:
#    adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Known emulator issue:** Raw `adb install` on Pixel 7 API 34 sometimes fails with "Error occurred while checking alignment of package". This is an emulator-side PackageManager quirk, not a code problem. Workaround: run from Android Studio instead, or wipe-data the emulator.

Expected on first launch:
- Reader status bar shows "No RFID Reader (Emulator Mode)" in grey
- Home screen shows 4 tiles
- All navigation works
- Settings → Field Configuration → template buttons change the field list dynamically
- Individual Linking renders domain-specific fields based on current field config

For any requirement question, check `docs/srs/RFID_App_SRS_C72.md` first. For any scope question (is this in MVP or not?), check `docs/srs/StockBuddy_MVP_Demo_Scope.md`. For layout/UX questions, check `docs/srs/referenceimages/` for context, but defer to the NFR-10 series rules over anything you see in those images.

---

## Reference documents

All reference documents live under `docs/srs/` relative to the project root.

### Specification documents

| Path | Version | Purpose |
|---|---|---|
| `docs/srs/RFID_App_SRS_C72.md` | v3.14 | **Primary source of truth.** Every FR/NFR cited in code comments traces here. Consult before making any requirement-level decision. |
| `docs/srs/StockBuddy_MVP_Demo_Scope.md` | current | MVP scope decisions: what is in scope, what is explicitly cut, testing strategy (emulator vs physical C72), demo mode limits rationale, build tooling decisions. |
| `docs/srs/StockBuddy_Test_Values_RFID_Barcode.md` | current | 20 sample RFID EPC hex values and 20 sample EAN-13 barcodes with valid check digits, for manual entry during emulator-based testing before physical tags are in hand. Includes suggested test scenarios (grouping, duplicate-RFID rejection, Missing-item detection). |

### Reference images (`docs/srs/referenceimages/`)

Screenshots from a prior jewellery-client deployment of a related RFID app. These are **UI/UX reference only** — no source code from that app is used or ported. Use these images to:
- Understand the expected screen layout and information density for a rugged-handheld context
- Inform layout decisions when the SRS describes a screen but leaves layout specifics open
- Verify that any new screen you build feels consistent with the established visual style

Consult reference images when building or reviewing: the Individual Linking form, the Inventory scanning screen, the Results summary screen, and the Assets list. Do not replicate any branding, colour scheme, or domain-specific content from those images — only the structural/layout patterns are relevant.

When a reference image and an NFR conflict (e.g. the image shows a layout that violates the thumb-zone rule NFR-10d), **the NFR wins**. The images predate the UI rules established for StockBuddy.
