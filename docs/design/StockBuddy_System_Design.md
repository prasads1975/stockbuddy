# StockBuddy — System Design Document
**Application:** StockBuddy RFID Inventory Management  
**Platform:** Android (Java) · Chainway C72 Handheld UHF RFID Reader  
**Client:** Crazy Hamster Toy Store, Yavatmal  
**SRS Version:** 3.6 (Draft) · June 2026  
**Design Version:** 1.0

---

## Table of Contents

1. [Assumptions & Clarifications](#1-assumptions--clarifications)
2. [High-Level Architecture](#2-high-level-architecture)
3. [Component Design](#3-component-design)
4. [Data Model](#4-data-model)
5. [Hardware Integration Layer](#5-hardware-integration-layer)
6. [Authentication & RBAC](#6-authentication--rbac)
7. [Licensing Subsystem](#7-licensing-subsystem)
8. [Report Export & Delivery](#8-report-export--delivery)
9. [Backup & Restore](#9-backup--restore)
10. [Screen Navigation & State](#10-screen-navigation--state)
11. [Non-Functional Requirements Mapping](#11-non-functional-requirements-mapping)
12. [Trade-off Analysis](#12-trade-off-analysis)
13. [Open Items Impact on Design](#13-open-items-impact-on-design)
14. [What to Revisit as the System Grows](#14-what-to-revisit-as-the-system-grows)

---

## 1. Assumptions & Clarifications

The following assumptions underpin this design:

| # | Assumption | Rationale |
|---|-----------|-----------|
| A1 | QR payload (if used) is JSON-encoded with keys matching field names defined in Field Config metadata | Enables flexible domain-specific field binding |
| A2 | Barcode (EAN-13 or equivalent) is the primary business identifier for product grouping and matching | Industry standard; more stable than article ID across franchises |
| A3 | Article ID (if present) is business-assigned supplementary data only, never used as a uniqueness or grouping key | Optional; controlled by field configuration, not schema design |
| A4 | RFID tag EPC is the guaranteed-unique identifier for each physical unit | Enforced at DB layer via PK constraint (NFR-18) |
| A5 | Each inventory session is tied to a single device instance; no multi-device sync needed (MVP only) | Simplifies session model; future: distributed UUIDs enable multi-device |
| A6 | Excess items ARE included in exported CSV as a third section with status="Excess" | Required for complete inventory audit trail (FR-35) |
| A7 | Product catalogue can be updated post-scan without invalidating historical session snapshots | `session_result_items` captures immutable snapshot at STOP time (§4.2.3) |
| A8 | Domain-specific fields (price, location, serial number, etc.) are defined per-franchise at runtime | Schema is polymorphic; field definitions stored in `field_definitions` table |

---

## 2. High-Level Architecture

StockBuddy is a **local-first, single-device Android application**. There is no application server, no remote database, and no backend API owned by the app. All data lives on the C72. The only outbound network calls are user-initiated (report delivery) or periodic background-only (revocation list fetch).

### 2.1 Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      UI LAYER                           │
│  Activities / Fragments + ViewModels (MVVM)             │
│  Material Design 3 · Java · min SDK 28 (Android 9)      │
└────────────────────────┬────────────────────────────────┘
                         │ LiveData / StateFlow
┌────────────────────────▼────────────────────────────────┐
│                   DOMAIN LAYER                          │
│  Use Cases / Interactors                                │
│  Business rules: validation, dedup, result computation  │
└───────┬──────────────────────────────────┬──────────────┘
        │                                  │
┌───────▼───────┐                ┌─────────▼──────────────┐
│  REPOSITORY   │                │  HARDWARE ABSTRACTION  │
│  LAYER        │                │  LAYER                 │
│  Single source│                │  RFIDManager           │
│  of truth     │                │  ImagerManager         │
└───────┬───────┘                └─────────┬──────────────┘
        │                                  │
┌───────▼───────┐                ┌─────────▼──────────────┐
│  DATA LAYER   │                │  CHAINWAY SDK           │
│  Room DB      │                │  (UHF + Imager APIs)   │
│  DataStore    │                └────────────────────────┘
│  FileSystem   │
└───────────────┘
```

### 2.2 Network Topology

```
┌────────────────────────────────────┐
│         Chainway C72               │
│                                    │
│  ┌──────────┐   ┌───────────────┐  │
│  │StockBuddy│   │  SQLite (Room)│  │
│  │   APK    │   │  + Files      │  │
│  └────┬─────┘   └───────────────┘  │
│       │                            │
└───────┼────────────────────────────┘
        │ (user-triggered or periodic background)
   ─────┼──────────────────────────────────────────
        │
        ├──► Internet: Revocation List JSON (GET, every 7 days)
        ├──► Internet: API Upload endpoint (POST CSV, user-triggered)
        └──► LAN: Store Wi-Fi → Back-office server / NAS (user-triggered)
```

---

## 3. Component Design

### 3.1 Module Map

```
StockBuddy
├── :app                    (entry point, navigation graph, DI setup)
├── :feature:linking        (Individual Linking, Bulk Linking, Product Master)
├── :feature:inventory      (Session mgmt, RFID scanning, results)
├── :feature:assets         (Assets list, search, detail, delete)
├── :feature:settings       (Auth config, categories, field config, backup, license)
├── :feature:export         (CSV generation, share, API upload, LAN transfer)
├── :feature:setup          (First-run wizard: 4 steps)
├── :core:db                (Room database, DAOs, entities, migrations)
├── :core:hardware          (Chainway RFID + imager abstraction)
├── :core:license           (Token parsing, signature verification, revocation)
├── :core:auth              (PIN/password hashing, session management, RBAC)
├── :core:backup            (Snapshot, zip, encrypt, restore)
└── :core:common            (Field config engine, CSV parser/writer, shared models)
```

### 3.2 Key Component Responsibilities

#### 3.2.1 Field Configuration Engine (`:core:common`)

This is the most cross-cutting component. It drives dynamic rendering across Linking, Assets, Inventory, and CSV export.

```
FieldConfigEngine
  ├── loadFieldDefinitions()         → List<FieldDefinition>
  ├── getFieldsFor(screen: Screen)   → List<FieldDefinition>  (filtered by visibility flags)
  ├── parseFromQR(json: String)      → Map<String, Any?>
  ├── validateRecord(map)            → ValidationResult
  ├── buildCsvHeaders(scope: Scope)  → List<String>
  └── extractDomainAttributes(map)   → JSON string (for DB storage)

data class FieldDefinition(
  val key: String,
  val label: String,
  val dataType: FieldType,           // TEXT | NUMBER | DROPDOWN | DATE
  val mandatory: Boolean,
  val visibleOnLinkingForm: Boolean,
  val visibleOnAssetsList: Boolean,
  val visibleOnInventorySummary: Boolean,
  val visibleOnCsvExport: Boolean,
  val dropdownValues: List<String>?  // null unless dataType == DROPDOWN
)
```

Stored as a single JSON record in the `field_config` table. Loaded once at app start, cached in memory, observed via LiveData so screens re-render if config changes.

**`saveLinkedItem()` — mandatory field enforcement (FR-09, FR-09a)**

Validation runs in `ItemRepository.saveLinkedItem()` before any DB write and covers two tiers:

```kotlin
sealed class SaveResult {
  object Success : SaveResult()
  data class ValidationError(val fieldErrors: Map<String, String>) : SaveResult()
  object DuplicateRfid : SaveResult()
  object DemoLimitReached : SaveResult()
}

fun saveLinkedItem(...): SaveResult {
  errors = mutableMapOf<String, String>()

  // Tier 1 — fixed mandatory fields (FR-09)
  val fixedFields = mapOf(
    "productName" to productName,
    "barcode" to barcode,
    "category" to category,
    "rfidTagId" to rfidTagId
  )
  fixedFields.forEach { (key, value) ->
    if (value.isBlank()) errors[key] = "$key is required"
  }

  // Tier 2 — domain-specific mandatory fields (FR-09a)
  fieldDefinitions.filter { it.mandatory }.forEach { fieldDef ->
    if (attributes[fieldDef.key].isNullOrBlank())
      errors["attr_${fieldDef.key}"] = "${fieldDef.label} is required"
  }

  if (errors.isNotEmpty()) return SaveResult.ValidationError(errors)
  
  // Tier 3 — RFID uniqueness check
  if (linkedItemDao.existsByRfid(rfidTagId)) return SaveResult.DuplicateRfid
  
  // Tier 4 — demo limit check
  if (linkedItemDao.count() >= DemoLimits.MAX_ITEMS) return SaveResult.DemoLimitReached
  
  // All checks passed — insert
  linkedItemDao.insert(LinkedItemEntity(...))
  productDao.upsertFromLinkedItem(...)  // auto-upsert Product Master
  return SaveResult.Success
}
```

If `ValidationResult.valid` is false, `IndividualLinkingViewModel` emits a `ValidationFailedEvent` containing `fieldErrors`. The UI highlights each failing field with an inline error message below it. The save is blocked until `validateRecord()` returns `valid = true`.

**Mandatory field indication — rendered at form load (FR-09b)**

Mandatory status is displayed proactively at form load, not only after a failed save attempt. `FieldConfigEngine.getFieldsFor(LINKING_FORM)` returns each `FieldDefinition` with its `mandatory` flag. The form adapter uses this flag to append an asterisk to the field label at render time:

```
label = if (fieldDef.mandatory) "${fieldDef.label} *" else fieldDef.label
```

The same rule applies to fixed fields — their `mandatory` flag is hardcoded `true` in the fixed-field definitions. A legend line *"* Required field"* is rendered once below the form title (not repeated per field). This ensures the user sees which fields are required before attempting to save, reducing save-time validation errors.

#### 3.2.2 RFID Session Engine (`:feature:inventory`)

Manages the lifecycle of an active scan session. Decouples the hardware read loop from the UI.

```
RfidSessionEngine
  ├── startSession(inventoryCode, sessionId)
  ├── stopSession() → SessionSnapshot
  ├── tagFlow: Flow<String>           (emits each new unique EPC, deduplicated)
  ├── uniqueTagCount: StateFlow<Int>
  └── recoverSession(sessionId)       (crash recovery — NFR-07)

SessionSnapshot
  ├── sessionId: String
  ├── inventoryCode: String
  ├── stoppedAt: Long                 (epoch ms)
  ├── rawTagSet: Set<String>          (all unique EPCs collected)
  └── categoryFilter: String?
```

Deduplication is done with an in-memory `HashSet<String>` per session. The raw tag set is persisted to `session_tags` table on every N tags (configurable, default 50) as a crash-recovery buffer (NFR-07).

#### 3.2.3 Results Computation Engine (`:feature:inventory`)

Computes Available / Missing / Excess after STOP. Runs on a background thread (< 3 seconds for 10,000 items per NFR-02).

```
ResultsComputationEngine
  fun computeResults(sessionId: String, categoryFilter: String?): List<ResultItem>

Algorithm:
  1. Load all scanned tags from session_tags table → Set<String>
  2. Load all LinkedItem records from DB → List<LinkedItem>
  3. For each LinkedItem in master:
     - If rfid_tag_id in scanned → Status.AVAILABLE (check category filter)
     - Else                      → Status.MISSING (check category filter)
  4. For each scanned tag not in master → synthetic LinkedItem, Status.EXCESS
  5. Category filter applied at display time; raw scan set never discarded (FR-46)

ResultItem
  ├── item: LinkedItemEntity
  └── status: Status  (AVAILABLE | MISSING | EXCESS)
```

#### 3.2.5 Single-Shot Scan Engine (`:feature:linking`)

Handles the one-shot UHF scan triggered by the **"Scan"** button in the Individual Linking manual form (FR-07a). This is separate from `RfidSessionEngine` (§3.2.2), which runs a continuous multi-tag inventory loop for Inventory sessions. The single-shot engine is designed for precision: exactly one tag must be returned for the result to be accepted.

```
SingleShotScanEngine
  ├── scan(): SingleShotResult
  │     // 1. Set UHF power to minimum (e.g. 5 dBm) via uhfReader.setPower()
  │     // 2. Call uhfReader.inventoryOnce() — single burst, returns List<String> EPCs
  │     // 3. Restore UHF power to session default
  │     // 4. Return result based on EPC count
  └── (stateless — no session, no persistence)

SingleShotResult (sealed class)
  ├── Success(epc: String)          // exactly 1 tag detected
  ├── NoTagDetected                 // 0 tags detected
  └── MultipleTagsDetected(count: Int)  // 2+ tags detected
```

**ViewModel handling** (`IndividualLinkingViewModel`):

```kotlin
fun onScanButtonPressed() {
    viewModelScope.launch(Dispatchers.IO) {
        when (val result = singleShotScanEngine.scan()) {
            is Success             -> rfidTagId.value = result.epc
            is NoTagDetected       -> uiEvent.emit(ScanError.NoTag)
            is MultipleTagsDetected -> uiEvent.emit(ScanError.MultipleTagsInRange(result.count))
        }
    }
}
```

**UI feedback** (mapped to string resources, never raw exception text — NFR-13):

| Result | Message shown inline below RFID Tag ID field |
|--------|----------------------------------------------|
| No tag | *"No tag detected. Hold the device close to the tag and try again."* |
| Multiple tags | *"Multiple tags in range (N detected). Remove other tags from the scanning area and try again."* |
| Success | Field populated with EPC; no message shown |

**Power management note:** UHF transmit power on the C72 is set via `uhfReader.setPower(dBm)`. Minimum power (~5 dBm) reduces the effective read range to ≈5–10 cm, ensuring only the tag held directly against the device antenna is read. Power is always restored to the application default (typically 26–30 dBm) after the burst, so the setting does not persist into any subsequent inventory session.

---

#### 3.2.4 CSV Engine (`:core:common`)

Handles both import (Bulk Linking, Product Master, Category bulk) and export (Inventory Report, Product Master).

```
CsvEngine
  ├── parseImport(uri: Uri, requiredColumns: List<String>): CsvImportResult
  ├── generateInventoryReport(session, results, scope, fieldDefs): File
  ├── generateProductMasterExport(products, fieldDefs): File
  └── previewRows(uri: Uri, count: Int): List<Map<String,String>>

Export CSV column order:
  Status | ProductName | Barcode | Category | [domain fields in sortOrder] | RFID Tag ID

File naming: {InventoryCode}_{yyyyMMdd}_{HHmm}.csv  (spaces → underscores)
```

---

## 4. Data Model

### 4.1 Entity Relationship Diagram

```
┌──────────────────────┐          ┌────────────────────────┐
│  product_master      │ 1 ─────< │    linked_items        │
│──────────────────────│          │────────────────────────│
│ barcode (PK)         │          │ rfid_tag_id (PK)       │
│ product_name         │          │ barcode (FK)           │
│ category             │          │ product_name (denorm)  │
│ attributes JSON      │          │ category (denorm)      │
│ created_at           │          │ attributes JSON        │
│ updated_at           │          │ linked_at              │
└──────────────────────┘          │ linked_by_role         │
                                  └────────┬───────────────┘
                                           │
                                           │ (via session_tags)
                                  ┌────────▼──────────────┐
    ┌──────────────────┐          │ inventory_sessions    │
    │  session_tags    │ <──── 1  │────────────────────────│
    │──────────────────│          │ id (PK, TEXT UUID)    │
    │ session_id (FK)  │          │ inventory_code        │
    │ rfid_tag_id      │          │ started_at            │
    │ (crash buffer)   │          │ stopped_at (nullable) │
    └──────────────────┘          │ category_filter       │
                                  │ total_in_master       │ ← KPIs computed at STOP
                                  │ total_scanned         │
                                  │ available_count       │
                                  │ missing_count         │
                                  │ excess_count          │
                                  │ filtered_available    │
                                  │ filtered_missing      │
                                  │ filtered_excess       │
                                  └────────┬──────────────┘
                                           │ 1
                                           │ has many
                                           ▼ *
                                  ┌────────────────────────┐
                                  │ session_result_items   │ (immutable)
                                  │────────────────────────│
                                  │ id (PK)                │
                                  │ session_id (FK)        │
                                  │ status                 │ AVAILABLE|MISSING|EXCESS
                                  │ rfid_tag_id            │
                                  │ product_name           │ ┐ null for
                                  │ barcode                │ ├ EXCESS rows
                                  │ category               │ ┘
                                  │ attributes JSON        │
                                  └────────────────────────┘

                   ┌──────────────────────┐
                   │  categories          │
                   │──────────────────────│
                   │ id (PK)              │
                   │ name (UNIQUE)        │
                   │ created_at           │
                   └──────────────────────┘

                   ┌──────────────────────┐
                   │  field_definitions   │
                   │──────────────────────│
                   │ id (PK)              │
                   │ key                  │
                   │ label                │
                   │ data_type            │
                   │ mandatory            │
                   │ dropdown_options_csv │
                   │ sort_order           │
                   └──────────────────────┘

                   ┌──────────────────────┐
                   │  generated_reports   │
                   │──────────────────────│
                   │ id (PK)              │
                   │ session_id (FK)      │
                   │ file_name            │
                   │ file_path            │
                   │ export_scope         │
                   │ category_filter      │
                   │ generated_at         │
                   │ file_size_bytes      │
                   │ row_count            │
                   └──────────────────────┘

                   ┌──────────────────────┐
                   │  app_config          │
                   │──────────────────────│
                   │ key (PK)             │
                   │ value                │
                   └──────────────────────┘
```

### 4.2 Schema Constraints and Index Register

#### 4.2.1 Table Constraints (PKs, FKs, UNIQUE, NOT NULL)

Constraints are declared in Room via `@Entity(foreignKeys = […], indices = […])` and column-level annotations. The table below is the authoritative specification; the Room `@Entity` code must match it exactly.

| Table | PK | Foreign Keys (ON DELETE) | UNIQUE | NOT NULL columns |
|-------|----|--------------------------|--------|-----------------|
| `product_master` | `barcode` (TEXT) | — | — | `barcode`, `product_name`, `category` |
| `linked_items` | `rfid_tag_id` (TEXT) | `barcode → product_master(barcode)` CASCADE | — | `rfid_tag_id`, `barcode`, `product_name`, `category` |
| `inventory_sessions` | `id` (TEXT, UUID) | — | — | `id`, `inventory_code`, `started_at` |
| `session_tags` | composite (`session_id`, `rfid_tag_id`) | `session_id → inventory_sessions(id)` CASCADE | — | `session_id`, `rfid_tag_id` |
| `session_result_items` | `id` (INTEGER, autoincrement) | `session_id → inventory_sessions(id)` CASCADE | — | `session_id`, `status`, `rfid_tag_id` |
| `categories` | `id` (INTEGER, autoincrement) | — | `name` | `name` |
| `field_definitions` | `id` (INTEGER, autoincrement) | — | — | `id` |
| `generated_reports` | `id` (INTEGER, autoincrement) | `session_id → inventory_sessions(id)` CASCADE | — | `session_id`, `export_scope`, `generated_at` |
| `app_config` | `key` (TEXT) | — | — | `key` |

**Notes on FK behaviour choices:**
- `linked_items.barcode` uses CASCADE: because `barcode` is a mandatory (NOT NULL) column, SET NULL is not permitted. The application layer enforces the confirmation dialog before deletion; the DB CASCADE then removes the linked units atomically in the same transaction.
- All child tables of `inventory_sessions` use CASCADE so a session delete (FR-20[SM], G-09) removes all associated rows in one operation without requiring application-layer cleanup loops.

**Audit log retention policy (NFR-20o)**

The `audit_log` table uses the same 90-day rolling retention window as session data (NFR-19a), configurable by Admin (30–365 days, stored in `app_config` as `audit_log_retention_days`). A `WorkManager` daily `PeriodicWorkRequest` purges old entries:

```java
// AuditLogPurgeWorker (runs daily, piggybacks on the daily backup window)
long cutoff = System.currentTimeMillis() - (retentionDays * 86_400_000L);
db.auditLogDao().deleteEntriesOlderThan(cutoff);
// SQL: DELETE FROM audit_log WHERE timestamp < :cutoff
```

Index I-13 (`audit_log.timestamp`) serves both the purge query and the chronological listing view — no additional index needed. Storage estimate at 90-day retention with ~60 entries/day: ≈ 1.4 MB — well within the 500 MB storage budget (NFR-16).

**Mandatory field exception — `session_result_items`:**
The snapshot columns `product_name`, `barcode`, and `category` in `session_result_items` are **NOT NULL for AVAILABLE and MISSING rows** (which are matched to master records) but are **intentionally NULL for EXCESS rows** (tags found during scanning that do not exist in the product master — there is no master record to snapshot). This constraint cannot be expressed at the SQLite column level; it is enforced at the application layer in `ResultsComputationEngine` when writing the snapshot (§3.2.3).

**CHECK constraints** (enforced via Room `@ColumnInfo` or trigger):

| Table | Column | Allowed values |
|-------|--------|---------------|
| `session_result_items` | `status` | `AVAILABLE`, `MISSING`, `EXCESS` |
| `generated_reports` | `export_scope` | `FULL`, `FILTERED` |

**Domain-specific fields** are stored as `attributes TEXT` (JSON object, e.g. `{"price":"499.00"}`). SQLite JSON1 functions (`json_extract`, `json_set`) are used for reads and writes. Filtering on domain-specific fields is not indexed — acceptable at local-only scale (< 10,000 records).

---

#### 4.2.2 Index Register

Every index below is declared in the `indices` array of the relevant `@Entity` annotation. The "Access Pattern" column is the primary reason the index exists; secondary uses are noted where relevant.

| # | Table | Column(s) | Type | Access Pattern | Requirement |
|---|-------|-----------|------|----------------|-------------|
| I-01 | `linked_items` | `rfid_tag_id` | PK | Scan-time tag lookup during results computation; RFID uniqueness enforcement on save/import | NFR-18; FR-08 |
| I-02 | `linked_items` | `barcode` | — | Group results by barcode in inventory summary (FR-33); propagate product edits to all linked units | FR-33; FR-26 |
| I-03 | `linked_items` | `category` | — | Category filter on Assets screen; category filter for filtered KPIs in results | FR-55; FR-23 |
| I-04 | `inventory_sessions` | `stopped_at` | — | Reverse-chronological session list (`ORDER BY CASE WHEN stopped_at IS NULL THEN 0 ELSE 1 END ASC, stopped_at DESC`) | FR-19[SM] |
| I-05 | `session_tags` | `session_id` | — | Load all buffered tags for a session on crash recovery; used during `stopSession()` to retrieve the full tag set | NFR-07 |
| I-06 | `session_result_items` | `session_id` | — | Load all result rows for a session when opening historical summary | FR-32 |
| I-07 | `session_result_items` | `session_id`, `status` | Composite | Filter result rows by status tab (Available / Missing / Excess) without full table scan | FR-33; FR-34; FR-35 |
| I-08 | `generated_reports` | `session_id` | — | Look up all exports for a session (re-export FR-46; purge gate NFR-19a) | FR-46; NFR-19a |
| I-09 | `generated_reports` | `generated_at` | — | WorkManager 30-day CSV purge: `WHERE generated_at < cutoff AND file_path IS NOT NULL` | NFR-17 |

**Room `@Entity` example — `linked_items`** (shows how constraints and indexes are declared):

```kotlin
@Entity(
  tableName = "linked_items",
  foreignKeys = [
    ForeignKey(
      entity = ProductMasterEntity::class,
      parentColumns = ["barcode"],
      childColumns = ["barcode"],
      onDelete = ForeignKey.CASCADE  // barcode is NOT NULL — SET_NULL not permitted
    )
  ],
  indices = [
    Index(value = ["barcode"]),      // I-02: FK lookup; product edits propagation
    Index(value = ["category"])      // I-03: category filtering
  ]
)
data class LinkedItemEntity(
  @PrimaryKey
  @ColumnInfo(name = "rfid_tag_id")
  val rfidTagId: String,             // PK: RFID tag is the unique physical item identifier

  val barcode: String,               // FK to product_master (business identifier)
  @ColumnInfo(name = "product_name")
  val productName: String,           // Denormalized from product_master for scan-time O(1) lookup
  val category: String,              // Denormalized from product_master for filtering

  @ColumnInfo(name = "attributes")
  val attributesJson: String = "{}",  // Domain-specific fields (JSON)

  val linkedAt: Long = System.currentTimeMillis(),
  val linkedByRole: String? = null
)
```

All other `@Entity` classes follow the same pattern: declare `foreignKeys` and `indices` on the class annotation; use `@NonNull` / `@Nullable` to express NOT NULL; use `unique = true` on `@Index` for UNIQUE constraints.

---

#### 4.2.3 Design Decision — Denormalized Columns in `linked_items`

**Decision:** `linked_items` stores `product_name` and `category` as direct columns (keyed by barcode FK) rather than deriving them from `product_master` via JOIN at query time. This is **intentional and justified for MVP performance** — later revisable if storage becomes a constraint.

**Primary rationale: Scan-time performance optimization**

The hot path during an inventory session looks up each detected RFID tag with `SELECT … FROM linked_items WHERE rfid_tag_id = ?` (PK lookup). At 10+ scans/second (NFR-06), a JOIN to `product_master` on every match adds 30–50% overhead. With denormalized columns, one indexed PK lookup returns everything the results computation needs in a single row — eliminating the JOIN cost entirely.

**Quantified impact:**
- **Without denormalization:** SELECT + JOIN = ~3–4ms per scan × 50 items = 150–200ms aggregate
- **With denormalization:** Direct PK lookup = ~1–1.5ms per scan × 50 items = 50–75ms aggregate
- **Net saving:** ~50–60% faster scan path

**Secondary rationale: Historical snapshot immutability**

`session_result_items` captures a point-in-time snapshot of each item's product data at the moment STOP is pressed. This snapshot must reflect what the product was called *at scan time*, not its current name. If the snapshot only stored a FK and joined at view time, renaming a product after the session would silently alter what every historical report shows — incorrect for stock audit purposes.

**Maintenance contract this creates**

Denormalization is safe only if every write path that changes catalogue data also propagates to `linked_items`. These paths are explicitly designed and must not be broken:

| Write operation | Tables updated | Enforced by |
|----------------|----------------|-------------|
| Edit product (name / category) | `product_master` AND `linked_items WHERE barcode=?` | Repository `@Transaction` (FR-26) |
| Delete category + optional replacement | `product_master.category` AND `linked_items.category` | Repository batch UPDATE (FR-62) |
| Bulk Product Master CSV import | `product_master` only — `linked_items` unchanged unless explicitly re-linked | FR-22 upsert logic |

> **Risk:** If a write transaction is modified and the `UPDATE linked_items` leg is dropped, `product_master` and `linked_items` will silently diverge. The Assets screen, CSV export, and inventory results will show stale product names. Code reviews for any Repository change touching product data must verify all affected tables are updated.

**Storage impact (acceptable for MVP):**
- 50 linked items × (string product_name ~20 bytes + string category ~15 bytes) = ~1.75 KB additional overhead
- Well within SQLite limits; revisit if dataset scales to 10k+ items

#### 4.2.5 Report File Lifecycle

When the user exports a report (S14), `CsvEngine` writes the CSV to `{app_private_dir}/exports/{filename}.csv` and inserts one row into `generated_reports` recording the path, scope, and row count. Each delivery attempt (share, API, LAN) adds one row to `delivery_history` linked to that report record. This enables:

- **Re-export** (FR-46) — file path retrieved from `generated_reports.file_path`
- **Delivery history display** (FR-47) — joined via `report_id` (I-11) or `session_id` (I-12)
- **Session purge gate** (NFR-19a) — checked via `generated_reports WHERE session_id = ? AND generated_at IS NOT NULL`
- **30-day CSV auto-purge** (NFR-17) — WorkManager queries `generated_at < cutoff` (I-10), deletes the file from disk, nulls `file_path` (DB row kept for audit)

### 4.3 Room Database

```kotlin
@Database(
  entities = [
    CategoryEntity::class,
    FieldDefinitionEntity::class,
    ProductMasterEntity::class,
    LinkedItemEntity::class,
    InventorySessionEntity::class,
    SessionTagEntity::class,
    SessionResultItemEntity::class,
    GeneratedReportEntity::class,
    AppConfigEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun categoryDao(): CategoryDao
  abstract fun fieldDefinitionDao(): FieldDefinitionDao
  abstract fun productMasterDao(): ProductMasterDao
  abstract fun linkedItemDao(): LinkedItemDao
  abstract fun inventorySessionDao(): InventorySessionDao
  abstract fun sessionTagDao(): SessionTagDao
  abstract fun sessionResultItemDao(): SessionResultItemDao
  abstract fun generatedReportDao(): GeneratedReportDao
  abstract fun appConfigDao(): AppConfigDao
}
```

**Schema version history:**
- **v1** (initial): Included audit_log and delivery_history (out of MVP scope)
- **v2** (current MVP): Finalized data model with barcode-keyed product_master, RFID PK on linked_items, denormalized scan fields, UUID session IDs

### 4.3.1 Schema Management Strategy

**Tooling choice: Room's built-in migration framework**

Flyway and Liquibase are server-side tools (JDBC/ODBC) with no meaningful Android/SQLite integration and are not used. Room provides everything needed: versioned migrations, schema export, and startup-time version enforcement — all without additional dependencies.

**Version numbering**

The `version` integer in `@Database` is the single source of truth for schema state. It starts at 1 and increments by 1 for every APK release that changes the schema. Skipping versions or reusing numbers is not permitted.

**Schema export (audit trail)**

`exportSchema = true` (already set above) causes Room to write a `schema/{version}.json` file into the module's `assets/` directory on each build. This file contains the full CREATE TABLE statements and index definitions for that version. These JSON files are committed to version control and serve as a permanent, human-readable history of every schema version ever shipped to a device.

**Migration types**

For simple, additive changes (new nullable column, new table), use `@AutoMigration` — Room generates the `ALTER TABLE` automatically:

```java
@Database(
  entities = { … },
  version = 2,
  exportSchema = true,
  autoMigrations = {
    @AutoMigration(from = 1, to = 2)   // e.g. added 'attributes' column to linked_items
  }
)
```

For any change that Room cannot infer (column rename, column removal, data backfill, table restructure), write an explicit `Migration` object:

```java
static final Migration MIGRATION_2_3 = new Migration(2, 3) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        // Example: add generated_reports table (added in v3 design)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS generated_reports (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "session_id TEXT NOT NULL, " +
            "file_path TEXT NOT NULL, " +
            "generated_at INTEGER NOT NULL, " +
            "FOREIGN KEY(session_id) REFERENCES inventory_sessions(id) ON DELETE CASCADE)"
        );
        db.execSQL("CREATE INDEX idx_reports_session ON generated_reports(session_id)");
    }
};
```

Migrations are registered at database build time:

```java
Room.databaseBuilder(context, StockBuddyDatabase.class, "stockbuddy.db")
    .addMigrations(MIGRATION_2_3, MIGRATION_3_4 /*, … */)
    .build();
```

**Rules**

| Rule | Detail |
|------|--------|
| Never use `fallbackToDestructiveMigration()` in release builds | This drops and recreates the DB, destroying all device data. Permitted in debug builds only. |
| Every schema change ships with a migration | If a migration is missing, Room throws `IllegalStateException` at startup on any upgraded device. |
| Migrations must be forward-only | Never alter or remove a previously shipped migration. Past `Migration` objects are permanent. |
| Test migrations | Use Room's `MigrationTestHelper` in `androidTest` to verify each migration runs cleanly against a real SQLite file at the previous version. |
| APK sideload updates must not wipe data | Satisfied automatically by the above rules (NFR-30). |

**Development workflow summary**

1. Change an `@Entity` class (add field, new entity, etc.).
2. Increment `version` in `@Database`.
3. Add `@AutoMigration` (simple case) or a new `Migration` object (complex case).
4. Build — Room writes the new `schema/{version}.json` to `assets/`.
5. Commit both the entity change and the new schema JSON.
6. Add a `MigrationTestHelper` test before merging.

---

### 4.3.2 MVP Data Model Finalization — Key Design Decisions

**Context:** The MVP data model underwent significant revision during implementation to align business domain concepts with database architecture. Below are the critical decisions made and their rationale.

#### Decision 1: Barcode as Product Master PK (not Article ID)

**What changed:** `product_master` PK moved from `article_id` (TEXT) to `barcode` (TEXT).

**Rationale:**
- **Industry standard:** EAN-13 barcodes are the universal product identifier across retail. Article ID is domain-specific and often optional.
- **Multi-franchise portability:** The headline feature is "one APK, 30-second reconfiguration for any retail domain." Using barcode enables cross-domain consistency without rework.
- **Data integrity:** Barcode is immutable (printed on the product); article ID may change at client discretion.
- **Foreign key stability:** Linked items reference the product via barcode, reducing risk of FK conflicts during master data imports.

**Impact:**
- `linked_items` now uses `barcode` as the FK to `product_master` (not `article_id`).
- Article ID becomes optional domain-specific metadata (stored in `attributes` JSON if needed by a particular franchise).
- All result grouping and CSV export use barcode as the product key.

#### Decision 2: RFID as Primary Key on linked_items (not artificial id)

**What changed:** `linked_items` PK changed from `id` (BIGINT autoincrement) + UNIQUE(rfid_tag_id) to direct PK on `rfid_tag_id` (TEXT).

**Rationale:**
- **Semantic correctness:** In the business domain, an RFID tag uniquely identifies a physical unit. The tag IS the primary key; an artificial surrogate PK adds no semantic value.
- **Simpler schema:** One less column per row (8 bytes saved per 50 items = 400 bytes overhead eliminated).
- **Direct lookups:** Query `SELECT * FROM linked_items WHERE rfid_tag_id = ?` is now a direct PK lookup (fastest query on any DB).
- **No performance regression:** String PK is 5–10% slower than BIGINT PK on very large tables (100k+), but MVP has ≤ 50 items by design limit. The semantic clarity outweighs the negligible cost.

**Trade-off (accepted):**
- Cannot INSERT a linked_items row without supplying the RFID tag value (cannot auto-generate a PK).
- RFID tag MUST be known before insertion — validated in UI and enforced in repository layer.

#### Decision 3: Denormalized Columns in linked_items (product_name, category)

**What changed:** `linked_items` stores copies of `product_name` and `category` from `product_master` in the same row.

**Rationale:**
- **Scan-time performance:** The hot path is `SELECT … FROM linked_items WHERE rfid_tag_id = ?` (executed 10+ times per second during scanning). A JOIN to `product_master` adds 30–50% latency per scan.
- **Quantified impact:** With denormalization, 50 scans = 50–75ms aggregate; without = 150–200ms. For a handheld device with limited CPU, this is material.
- **Historical snapshot integrity:** `session_result_items` captures product names as they were at scan time. Denormalization ensures the copy at link time matches what the user sees on screen.

**Maintenance contract:**
- Every `UPDATE` or `DELETE` on `product_master` must cascade to all linked `linked_items` rows with matching barcode.
- Enforced via Repository-layer `@Transaction` methods, not DB triggers (simpler to test and audit).
- Code review guideline: Any change to `ProductMasterRepository.update()` must include `UPDATE linked_items WHERE barcode = ?`.

**Storage impact (MVP-acceptable):**
- 50 items × (20 bytes for product_name + 15 bytes for category) = ~1.75 KB.
- Negligible for MVP; revisit if dataset grows to 10k+ items.

#### Decision 4: UUID Session IDs (not autoincrement)

**What changed:** `inventory_sessions.id` is now TEXT (UUID) instead of BIGINT autoincrement.

**Rationale:**
- **Distributed-system ready:** UUIDs are globally unique across devices and can be used for future API-based session syncing or multi-device scenarios without coordination.
- **Predictable values:** Client-generated UUIDs are easier to reason about in logs and debugging than opaque autoincrement sequences.
- **No performance cost:** UUID string lookups are equivalent to BIGINT lookups for MVP scale (< 50 sessions).

**Trade-off (accepted):**
- Cannot rely on `AUTOINCREMENT` to guarantee sequential IDs.
- Session ordering now uses `ORDER BY CASE WHEN stopped_at IS NULL THEN 0 ELSE 1 END ASC, stopped_at DESC` to put active (stopped_at IS NULL) sessions first, then reverse-chronological.

#### Decision 5: Out-of-MVP Tables Removed

**What changed:** `delivery_history`, `audit_log` removed from `@Database` entities.

**Rationale:**
- **Scope clarity:** FR-53–56 (API/LAN export) and FR-48 (audit logging) are explicitly post-MVP in `StockBuddy_MVP_Demo_Scope.md`.
- **Reduced complexity:** Removing unused tables cuts migration and test burden for MVP release.
- **Easy to add later:** Tables can be added in v3+ schema without breaking existing data.

**Future migration (when needed):**
```kotlin
@AutoMigration(from = 2, to = 3)  // when audit_log is added in v3
```

### 4.4 Sample Data

The following examples use a realistic Crazy Hamster scenario:
- 3 products in the master (Hot Wheels set, Ludo board game, Ride-On car)
- 5 linked physical units across those 3 products
- 1 inventory session named "3 June Morning"
  - 3 tags scanned: 2 found (Available), 1 not scanned (Missing), 1 unregistered (Excess)

---

#### `categories`

| id | name         | created_at          |
|----|-------------|---------------------|
| 1  | Hot Wheels   | 2026-06-01 09:00:00 |
| 2  | Board Game   | 2026-06-01 09:00:00 |
| 3  | Ride-On      | 2026-06-01 09:00:00 |
| 4  | Educational  | 2026-06-01 09:00:00 |
| 5  | Craft Kit    | 2026-06-01 09:00:00 |

---

#### `field_definitions`

Domain-specific field metadata (one row per field). Defines the schema for the `attributes` JSON column in other tables.

| id | key     | label         | type   | mandatory | dropdownOptionsCsv | showOnLinking | showOnAssets | showOnInventory | showOnCsv | sortOrder |
|----|---------|---------------|--------|-----------|--------------------|----|----|----|----|----|
| 1  | price   | Price (₹)     | NUMBER | true      | NULL               | true | true | true | true | 1 |
| 2  | location| Aisle/Shelf   | TEXT   | false     | NULL               | true | true | false| false| 2 |
| 3  | condition | Condition   | DROPDOWN | false   | New,Opened,Damaged | false| true | true | false| 3 |

> Each field definition controls visibility on the Linking form, Assets list, Inventory results, and CSV export.
> Dropdown fields specify their allowed values in `dropdownOptionsCsv` (e.g. "Option1,Option2,Option3").
> `sortOrder` determines the column order in CSV exports and form rendering.

---

#### `product_master`

| barcode (PK) | product_name              | category   | attributes JSON         | created_at          | updated_at          |
|--------------|--------------------------|-----------|------------------------|---------------------|---------------------|
| BC-HW-001    | Hot Wheels 5-Car Pack     | Hot Wheels | `{"price": "349.00"}`  | 2026-06-01 10:00:00 | 2026-06-01 10:00:00 |
| BC-LG-002    | Ludo Classic Board Game   | Board Game | `{"price": "199.00"}`  | 2026-06-01 10:05:00 | 2026-06-01 10:05:00 |
| BC-RC-003    | Champ Ride-On Car (Red)   | Ride-On    | `{"price": "4999.00"}` | 2026-06-01 10:10:00 | 2026-06-01 10:10:00 |

> One product record per SKU (EAN-13 barcode) — shared across all physical units of the same variant.

---

#### `linked_items`

Each row is one physical box/unit with its own RFID tag.

| rfid_tag_id (PK) | barcode    | product_name            | category   | attributes JSON        | linked_at           |
|------------------|-----------|------------------------|-----------|------------------------|---------------------|
| E200001234567890A1 | BC-HW-001 | Hot Wheels 5-Car Pack   | Hot Wheels | `{"price": "349.00"}` | 2026-06-02 09:15:00 |
| E200001234567890A2 | BC-HW-001 | Hot Wheels 5-Car Pack   | Hot Wheels | `{"price": "349.00"}` | 2026-06-02 09:16:00 |
| E200001234567890B1 | BC-LG-002 | Ludo Classic Board Game | Board Game | `{"price": "199.00"}` | 2026-06-02 09:20:00 |
| E200001234567890B2 | BC-LG-002 | Ludo Classic Board Game | Board Game | `{"price": "199.00"}` | 2026-06-02 09:21:00 |
| E200001234567890C1 | BC-RC-003 | Champ Ride-On Car (Red) | Ride-On    | `{"price":"4999.00"}` | 2026-06-02 09:30:00 |

> **Key design change:** `rfid_tag_id` is the PK (not a UNIQUE column on an artificial id). This directly models the business reality: one RFID tag = one physical unit.
> **Denormalization:** `product_name` and `category` are stored denormalized for ~50-60% faster RFID scan lookups (FK lookup to `product_master` would require a JOIN on every scan).
> **Grouping:** Multiple rows share the same `barcode` (e.g. A1 and A2 both are BC-HW-001 units) for CSV export and results grouping.

---

#### `inventory_sessions`

Session "3 June Morning": 5 items in master, 3 tags scanned, no category filter active.
- E200001234567890A1 → scanned ✅ (Available)
- E200001234567890B1 → scanned ✅ (Available)
- E200001234567890C1 → NOT scanned ❌ (Missing)
- E200001234567890FF → scanned but NOT in master ⚠️ (Excess)

| id (UUID)                            | inventory_code  | started_at          | stopped_at          | category_filter | total_in_master | total_scanned | available_count | missing_count | excess_count | filtered_available | filtered_missing | filtered_excess |
|--------------------------------------|----------------|---------------------|---------------------|-----------------|----------------|--------------|----------------|--------------|-------------|-------------------|-----------------|----------------|
| 550e8400-e29b-41d4-a716-446655440000 | 3 June Morning | 2026-06-03 09:00:00 | 2026-06-03 09:45:00 | NULL            | 5              | 3            | 2              | 3            | 1           | 2                 | 3               | 1              |

> `category_filter = NULL` means "All" was selected — filtered KPIs equal full session KPIs.
> `missing_count = 3`: items A2, B2, and C1 were not scanned.

---

#### `session_tags`  *(crash-recovery buffer)*

Raw EPCs collected during the scan — written incrementally every 50 tags.
Purged once `session_result_items` is populated successfully.

| session_id                           | rfid_tag_id        |
|--------------------------------------|--------------------|
| 550e8400-e29b-41d4-a716-446655440000 | E200001234567890A1 |
| 550e8400-e29b-41d4-a716-446655440000 | E200001234567890B1 |
| 550e8400-e29b-41d4-a716-446655440000 | E200001234567890FF |

---

#### `session_result_items`

Immutable snapshot written at STOP time. Product fields copied from `linked_items` as they were at that moment.

| id | session_id (UUID prefix) | status    | rfid_tag_id        | product_name            | barcode    | category   | attributes JSON        |
|----|--------------------------|-----------|--------------------|-----------------------|-----------|-----------|------------------------|
| 1  | 550e8400…                | AVAILABLE | E200001234567890A1 | Hot Wheels 5-Car Pack   | BC-HW-001 | Hot Wheels | `{"price": "349.00"}` |
| 2  | 550e8400…                | AVAILABLE | E200001234567890B1 | Ludo Classic Board Game | BC-LG-002 | Board Game | `{"price": "199.00"}` |
| 3  | 550e8400…                | MISSING   | E200001234567890A2 | Hot Wheels 5-Car Pack   | BC-HW-001 | Hot Wheels | `{"price": "349.00"}` |
| 4  | 550e8400…                | MISSING   | E200001234567890B2 | Ludo Classic Board Game | BC-LG-002 | Board Game | `{"price": "199.00"}` |
| 5  | 550e8400…                | MISSING   | E200001234567890C1 | Champ Ride-On Car (Red) | BC-RC-003 | Ride-On    | `{"price":"4999.00"}` |
| 6  | 550e8400…                | EXCESS    | E200001234567890FF | NULL                    | NULL       | NULL       | NULL                   |

> EXCESS row (id=6): tag was scanned but has no master record — all product fields are NULL.
> Immutability guarantee: Even if the product name in `product_master` is later edited, rows 1 and 3 retain the name as it was at scan time.
> Grouping: UI groups rows 1 and 3 by barcode (BC-HW-001 = 1 available, 1 missing), rows 2 and 4 by barcode (BC-LG-002 = 1 available, 1 missing).

**How S13 tabs are populated from this table:**

```sql
-- Available tab
SELECT * FROM session_result_items
WHERE session_id = '550e8400...' AND status = 'AVAILABLE'
-- Returns rows 1, 2

-- Missing tab
SELECT * FROM session_result_items
WHERE session_id = '550e8400...' AND status = 'MISSING'
-- Returns rows 3, 4, 5  (grouped by barcode in UI: BC-HW-001 × 1, BC-LG-002 × 1, BC-RC-003 × 1)

-- Excess tab
SELECT * FROM session_result_items
WHERE session_id = '550e8400...' AND status = 'EXCESS'
-- Returns row 6

-- Category filter applied (e.g. "Hot Wheels" only)
SELECT * FROM session_result_items
WHERE session_id = '550e8400...' AND status = 'MISSING' AND category = 'Hot Wheels'
-- Returns row 3 only
```

---

#### `generated_reports`

| id | session_id (prefix) | file_name                          | file_path                                    | export_scope | category_filter | generated_at        | file_size_bytes | row_count |
|----|--------------------|------------------------------------|----------------------------------------------|-------------|----------------|---------------------|----------------|----------|
| 1  | 550e8400…          | 3_June_Morning_20260603_1030.csv   | /data/data/com.gigakin.stockbuddy/exports/…  | FULL        | NULL           | 2026-06-03 10:30:00 | 4096           | 6        |


#### `app_config`  *(selected keys)*

| key                    | value                                          |
|------------------------|------------------------------------------------|
| admin_username         | prasad                                         |
| admin_password_hash    | $pbkdf2-sha256$100000$…                        |
| admin_salt             | a3f9c2d1…                                      |
| operator_pin_hash      | $pbkdf2-sha256$100000$…                        |
| operator_salt          | b7e4f1a2…                                      |
| operator_enabled       | true                                           |
| idle_timeout_mins      | 5                                              |
| session_retention_days | 90                                             |
| api_endpoint           | https://hq.crazyhamster.in/upload              |
| api_token              | Bearer eyJhbGci…                               |
| lan_endpoint           | http://192.168.1.10:8080/inventory             |
| license_token          | eyJhbGciOiJSUzI1NiJ9…                          |
| revocation_url         | https://gist.githubusercontent.com/gigakin/…  |
| revocation_cache       | `{"revokedTokens":[],"updatedAt":"2026-06-01"}`|
| revocation_fetched_at  | 2026-06-01 08:00:00                            |
| active_session_id      | NULL  ← null when no session in progress       |
| setup_complete         | true                                           |
| last_backup_at         | 2026-06-03 00:00:00                            |
| storage_budget_mb      | 500                                            |
| security_q1            | What is the name of your first school?         |
| security_a1_hash       | $pbkdf2-sha256$100000$…                        |
| security_q2            | What was the name of your first pet?           |
| security_a2_hash       | $pbkdf2-sha256$100000$…                        |

### 4.5 Design Gaps Addressed (from Traceability Matrix)

The following gaps (G-01 through G-14) were identified during traceability review and are resolved here.

---

#### G-01 & G-02 — Category Management Logic (FR-61, FR-62)

```java
class CategoryRepository {

  // FR-61: Add
  void add(String name) throws DuplicateCategoryException {
    // INSERT OR FAIL — Room throws SQLiteConstraintException on UNIQUE violation
    categoryDao.insert(new CategoryEntity(name.trim()));
  }

  // FR-61: Edit
  void update(int id, String newName) throws DuplicateCategoryException {
    categoryDao.update(id, newName.trim());   // UNIQUE constraint on name column
  }

  // FR-61 + FR-62: Delete
  CategoryDeleteImpact getDeleteImpact(String categoryName) {
    int linkedItemCount  = linkedItemDao.countByCategory(categoryName);
    int productMastCount = productMasterDao.countByCategory(categoryName);
    return new CategoryDeleteImpact(linkedItemCount, productMastCount);
  }

  // FR-62: Delete — called after Admin confirms warning dialog
  @Transaction
  void delete(String categoryName, @Nullable String replacementCategory) {
    if (replacementCategory != null) {
      // Update all referencing records to replacement category
      linkedItemDao.updateCategory(categoryName, replacementCategory);
      productMasterDao.updateCategory(categoryName, replacementCategory);
    }
    // If no replacement: records retain the deleted value as free text (FR-62)
    categoryDao.deleteByName(categoryName);
  }
}
```

ViewModel emits a `CategoryDeleteEvent` to the UI; UI shows the impact count and optional replacement dropdown before calling `delete()`.

---

#### G-03 — Product Edit Propagation to Linked Items (FR-26)

```kotlin
class ProductRepository(
  private val productMasterDao: ProductMasterDao,
  private val linkedItemDao: LinkedItemDao
) {

  @Transaction
  suspend fun updateProduct(barcode: String, name: String, category: String, attributesJson: String) {
    // 1. Update the product master record
    productMasterDao.update(barcode, name, category, attributesJson)
    // 2. Propagate to all linked physical units sharing this barcode (denormalization maintenance)
    linkedItemDao.updateProductFieldsByBarcode(barcode, name, category, attributesJson)
  }
}

// LinkedItemDao:
// UPDATE linked_items SET product_name=:name, category=:category,
//   attributes=:attrs WHERE barcode=:barcode
```

Both operations run in the same Room transaction — either both succeed or neither does. This is the maintenance contract for denormalized columns (§4.2.3).

---

#### G-04 — Product Delete with Cascade Warning (FR-27)

```kotlin
class ProductRepository(
  private val productMasterDao: ProductMasterDao,
  private val linkedItemDao: LinkedItemDao
) {

  suspend fun countLinkedItems(barcode: String): Int {
    return linkedItemDao.countByBarcode(barcode)
  }

  @Transaction
  suspend fun deleteProduct(barcode: String) {
    // Deletes linked items first (FK child), then product master (FK parent)
    // FK constraint with ON DELETE CASCADE could do this automatically, but explicit
    // control lets us emit an audit event before deletion
    linkedItemDao.deleteByBarcode(barcode)
    productMasterDao.deleteByBarcode(barcode)
  }
}
```

ViewModel flow: call `countLinkedItems(barcode)` → if > 0, emit `CascadeDeleteWarningEvent(count)` → UI shows confirmation dialog → on confirm, call `deleteProduct(barcode)`.

---

#### G-05, G-06, G-07, G-08 — Assets ViewModel (FR-51, FR-52, FR-54, FR-55)

```java
class AssetsViewModel extends ViewModel {

  // Inputs (from UI controls)
  private final MutableStateFlow<String>    searchQuery     = new MutableStateFlow<>("");
  private final MutableStateFlow<String>    categoryFilter  = new MutableStateFlow<>(null); // null = All
  private final MutableStateFlow<SortOrder> sortOrder       = new MutableStateFlow<>(SortOrder.NAME_ASC);

  enum SortOrder { NAME_ASC, ARTICLE_ID_ASC, LINKED_AT_DESC }

  // Outputs (observed by UI)
  public final StateFlow<List<LinkedItemEntity>> items;       // FR-49, FR-51, FR-54, FR-55
  public final StateFlow<Integer>                filteredCount; // FR-52

  AssetsViewModel(LinkedItemDao dao) {
    // Combine all filters into a single reactive query
    items = combine(searchQuery, categoryFilter, sortOrder, (q, cat, sort) -> {
      val like = "%$q%"
      return dao.search(like, cat, sort.toSqlOrderBy())
      // SELECT * FROM linked_items
      //   WHERE (product_name LIKE :like OR barcode LIKE :like
      //          OR rfid_tag_id LIKE :like)
      //   AND (:cat IS NULL OR category = :cat)
      //   ORDER BY <sort>
    })
    filteredCount = items.map { it.size }  // FR-52: real-time count
  }
}
```

---

#### G-09 — Delete Session: Cascade to Child Tables (FR-20[SM])

```java
class InventorySessionRepository {

  @Transaction
  void delete(String sessionId) {
    // 1. Retrieve file paths before deleting DB rows
    List<String> csvPaths = generatedReportDao.getFilePathsBySession(sessionId);

    // 2. Delete child table rows
    sessionResultItemDao.deleteBySession(sessionId);
    sessionTagDao.deleteBySession(sessionId);
    deliveryHistoryDao.deleteBySession(sessionId);
    generatedReportDao.deleteBySession(sessionId);

    // 3. Delete the session itself
    sessionDao.deleteById(sessionId);

    // 4. Delete CSV files from disk
    for (String path : csvPaths) {
      if (path != null) new File(path).delete();
    }
  }
}
```

---

#### G-10 — Rolling Debug Log (NFR-29)

```java
class DebugLogger {

  private static final int MAX_DAYS = 7;
  private final File logFile;  // {private_dir}/debug/debug.log

  void log(String tag, String message) {
    // Append: [2026-06-03 09:45:01] [tag] message\n
    // Rotate: on first write of the day, delete entries older than 7 days
  }

  File exportLog() {
    // Returns logFile via FileProvider URI for share sheet
  }
}
```

`DebugLogger` is injected into all Repositories and the Hardware layer. The export action is exposed in Settings → App Info → "Share Debug Log" (Admin only).

---

#### G-11 — Error Message Strategy (NFR-13)

All exceptions are caught at the ViewModel boundary and mapped to user-readable messages:

```java
// Pattern used in all ViewModels:
try {
  repository.someOperation();
} catch (DuplicateRfidException e) {
  errorEvent.emit("This RFID tag is already linked to " + e.getConflictingArticleId());
} catch (MissingMandatoryFieldException e) {
  errorEvent.emit("Please fill in: " + String.join(", ", e.getMissingFields()));
} catch (NetworkException e) {
  errorEvent.emit("Could not connect. Check your network and try again.");
} catch (Exception e) {
  debugLogger.log("ViewModel", "Unexpected error: " + e.getMessage());
  errorEvent.emit("Something went wrong. Please try again.");
}
```

Raw exception messages and stack traces are written to the debug log only — never surfaced to the UI (NFR-13).

---

#### G-12 — Reader Connected Status Indicator (SRS §4.6.2)

An application-scoped ViewModel monitors the Chainway SDK connection state and broadcasts it to all screens:

```java
// Application-scoped (survives screen navigation)
class RfidReaderStatusViewModel extends AndroidViewModel {

  public final StateFlow<Boolean> readerConnected;

  RfidReaderStatusViewModel(RfidReader reader) {
    readerConnected = reader.connectionState();  // StateFlow<Boolean> from Chainway SDK
  }
}
```

A shared toolbar component in the NavGraph's root layout observes `readerConnected` and shows a green "Reader Connected" chip or a red "Reader Disconnected" warning on every screen.

---

#### G-13 — Auto-Upsert Product Master on Item Save (FR-21)

```java
class LinkedItemRepository(
  private val linkedItemDao: LinkedItemDao,
  private val productMasterDao: ProductMasterDao
) {

  @Transaction
  suspend fun saveLinkedItem(
    productName: String, barcode: String, category: String,
    rfidTagId: String, attributesJson: String = "{}"
  ): SaveResult {
    // 1. Validate RFID uniqueness (NFR-18)
    if (linkedItemDao.existsByRfid(rfidTagId)) {
      return SaveResult.DuplicateRfid
    }

    // 2. Insert the linked item (RFID is the PK)
    linkedItemDao.insert(LinkedItemEntity(
      rfidTagId = rfidTagId,
      barcode = barcode,
      productName = productName,
      category = category,
      attributesJson = attributesJson
    ))

    // 3. Auto-upsert the Product Master (FR-21)
    //    Insert if barcode not present; update name/category/attributes if it is
    productMasterDao.upsert(ProductMasterEntity(
      barcode = barcode,
      productName = productName,
      category = category,
      attributesJson = attributesJson
    ))
    
    return SaveResult.Success
  }
}
```

Both operations share a single `@Transaction` — the product master is always in sync with linked items after every save. This maintains the denormalization invariant (§4.2.3).

---

#### G-15 — QR Scanner Auto Re-arm After Save (FR-11)

After a successful save in QR Scan mode, the scanner must re-arm automatically without the user navigating away:

```java
// In QR Scan ViewModel, observe save result:
void onSaveSuccess() {
  showConfirmationBriefly();          // flash "Saved" banner for ~1s
  clearFormFields();
  imager.startScan(this);             // re-arm immediately — FR-11
}
```

The `Imager.startScan()` call is made on the same screen after the success banner, keeping the user in rapid sequential scanning mode.

---

#### G-16 — Category Mismatch Dialog on Save (FR-12a)

When a Category value entered on the Linking form (manual or QR) does not match any entry in the `categories` table:

```java
// CategoryValidator (called inside LinkedItemRepository.save() before persisting):
ValidationResult validateCategory(String categoryValue) {
  boolean exists = categoryDao.existsByName(categoryValue.trim());
  if (!exists) return ValidationResult.categoryMismatch(categoryValue);
  return ValidationResult.ok();
}

// ViewModel handles the mismatch:
void onSaveRequested(LinkedItemFormData data) {
  ValidationResult result = categoryValidator.validateCategory(data.category);
  if (result.isCategoryMismatch()) {
    // Emit event — UI shows dialog with two options:
    // (a) "Add '[value]' to category list and save"
    // (b) "Choose an existing category"
    uiEvent.emit(new CategoryMismatchEvent(data.category));
  } else {
    repository.save(data);
  }
}

// If user picks option (a):
void onAddCategoryAndSave(String newCategory, LinkedItemFormData data) {
  categoryDao.insert(new CategoryEntity(newCategory));  // add to master list
  data.category = newCategory;
  repository.save(data);
}
```

---

#### G-17 — CsvImportResult Fields (FR-18[BL])

The `CsvImportResult` returned by `CsvEngine.parseImport()` is explicitly defined as:

```java
class CsvImportResult {
  int insertedCount;          // rows successfully inserted (new RFID)
  int updatedCount;           // rows successfully updated (existing RFID)
  int rejectedCount;          // rows skipped due to errors
  List<RejectedRow> rejected; // detail for each rejected row

  class RejectedRow {
    int    rowNumber;
    String rfidTagId;    // null if RFID column was blank
    String reason;       // e.g. "Missing RFID", "Duplicate RFID in file", "Missing Product Name"
  }
}
```

The UI displays a summary card (inserted / updated / rejected counts) and an expandable list of rejected rows with their reasons, matching FR-18[BL] and FR-24.

---

#### G-18 — Product Master View Query (FR-25)

The Product Master screen lists all products with a linked-unit count per product:

```kotlin
// ProductMasterDao:
@Query("""
  SELECT p.barcode, p.product_name, p.category, p.attributes,
         COUNT(l.rfid_tag_id) AS linked_count
  FROM product_master p
  LEFT JOIN linked_items l ON p.barcode = l.barcode
  WHERE (:search IS NULL OR p.product_name LIKE :search
         OR p.barcode LIKE :search OR p.category LIKE :search)
  GROUP BY p.barcode
  ORDER BY p.product_name ASC
""")
suspend fun searchWithLinkedCount(search: String?): List<ProductMasterSummary>

data class ProductMasterSummary(
  val barcode: String,              // PK: EAN-13 product identifier
  val productName: String,
  val category: String,
  val attributes: String,           // JSON — domain fields rendered via FieldConfigEngine
  val linkedCount: Int              // count of physical units linked to this SKU
)
```

The ViewModel exposes a `searchQuery: StateFlow<String>` that drives this query reactively, matching FR-25.

---

#### G-19 — Starter Templates Logic (FR-65l)

Starter templates are hardcoded constants in `FieldConfigEngine`. Selecting one pre-fills the domain field list in Step 3 of the setup wizard:

```java
// Hardcoded templates (P3 — nice to have, never restricts future changes):
enum FieldTemplate {
  NONE("Generic", Collections.emptyList()),

  TOY_RETAIL("Toy Retail", List.of(
    new FieldDefinition("price", "Price (₹)", FieldType.NUMBER, true,
      /*linking*/true, /*assets*/true, /*inventory*/true, /*csv*/true, null)
  )),

  JEWELLERY("Jewellery", List.of(
    new FieldDefinition("net_weight",   "Net Weight (g)",   FieldType.NUMBER, true,  true, true, true, true, null),
    new FieldDefinition("gross_weight", "Gross Weight (g)", FieldType.NUMBER, true,  true, true, true, true, null),
    new FieldDefinition("purity",       "Purity",           FieldType.TEXT,   true,  true, true, true, true, null)
  ));

  final String label;
  final List<FieldDefinition> fields;
}

// FieldConfigEngine:
List<FieldDefinition> getTemplateFields(FieldTemplate template) {
  return new ArrayList<>(template.fields);   // mutable copy — Admin can edit freely
}
```

---

#### G-20 — Single-Shot Scan for Individual Linking (FR-07a)

See §3.2.5 `SingleShotScanEngine` for the full design. Summary:

- Triggers a one-shot UHF inventory burst at minimum transmit power (~5 dBm).
- **0 tags** → inline error, field unchanged.
- **1 tag** → field populated with EPC.
- **2+ tags** → inline error with count, field unchanged — no auto-selection.
- Power restored to application default after burst regardless of outcome.

Selecting a template calls `getTemplateFields()` and populates the editable field list in the Step 3 UI. Admin can add, remove, or modify any field; the template is not persisted — only the final field definitions are.

---

#### G-14 — Two Separate CSV Export (FR-48, P3)

```java
// CsvEngine additions:
Pair<File, File> generateSplitReport(session, results, scope, fieldDefs) {
  File availableFile = writeCsv(results.available, "Available", ...);
  File missingFile   = writeCsv(results.missing,   "Missing",  ...);
  return Pair.of(availableFile, missingFile);
}

// DeliveryManager — split export:
// Share sheet: ZIP both files → share the ZIP
// API/LAN:     POST availableFile, then POST missingFile sequentially
//              Both delivery_history rows written; failure of second does not roll back first
```

This is a P3 (Nice to Have) feature and can be deferred to a later release.

---

## 5. Hardware Integration Layer

### 5.1 Abstraction Design

The Chainway SDK is wrapped behind interfaces to allow unit testing with mocks and future device portability.

```java
// UHF RFID Interface
public interface RfidReader {
  void startContinuousScan(RfidCallback callback);
  void stopScan();
  boolean isConnected();
  void release();
}

public interface RfidCallback {
  void onTagRead(String epc);
  void onError(RfidError error);
}

// Barcode / QR Imager Interface
public interface Imager {
  void startScan(ImagerCallback callback);
  void stopScan();
}

public interface ImagerCallback {
  void onBarcodeRead(String data, BarcodeFormat format);
  void onError(ImagerError error);
}

// Chainway-specific implementations
public class ChainwayRfidReader implements RfidReader { ... }
public class ChainwayImager implements Imager { ... }
```

### 5.2 RFID Read Loop

The C72 UHF reader is triggered via the device's physical side trigger button (which maps to a key event) and via the on-screen START button. Both routes call the same `startContinuousScan()`.

```
Physical trigger keydown  ──┐
                             ├──► RfidSessionEngine.start()
UI START button tap     ────┘         │
                                      ▼
                             ChainwayRfidReader.startContinuousScan()
                                      │
                             onTagRead(epc) → deduplicate → emit to Flow
                                      │
                             UI: count++ (within 1 second — NFR-01)
                                      │
                             Every 50 new tags: flush to session_tags table (crash buffer)
```

### 5.3 QR/Barcode Scanning

```
C72 Imager activated
        │
onBarcodeRead(data, format)
        │
   format == QR_CODE?
   ├── YES → FieldConfigEngine.parseFromQR(data)
   │          ├── All mandatory fields present? → pre-populate form, show for review (FR-03)
   │          └── Missing fields? → switch to manual form, pre-fill what was parsed (FR-04)
   └── NO (1D barcode) → populate Barcode field only (FR-06)
```

---

## 6. Authentication & RBAC

### 6.1 Credential Storage

All credentials stored as salted hashes using **PBKDF2 with HMAC-SHA256** (Android's `SecretKeyFactory`). Never stored in plain text (NFR-20j).

```
admin_password_hash  = PBKDF2(password, salt_admin, 100000 iterations)
operator_pin_hash    = PBKDF2(pin, salt_operator, 100000 iterations)
security_answer_hash = PBKDF2(answer.toLowerCase().trim(), salt_qa_N, 100000 iterations)

Salts: randomly generated at Admin setup time, stored in app_config table.
```

### 6.2 Authentication Flow

```
User enters PIN/password on S02
        │
Check against operator_pin_hash (PBKDF2 verify)
        │
   Match? ──► Login as OPERATOR
        │
   No match? → Check against admin_password_hash
        │
   Match? ──► Login as ADMIN
        │
   No match? → Increment failed_attempts counter
                │
            failed_attempts >= 5?
            └── Lock screen for cooldown_mins (default 5), show countdown (NFR-20c)
```

### 6.3 Session Management

After login, the current role is held in memory (never persisted). Auto-lock is implemented via an `IdleTimeoutManager` that observes `MotionEvent` forwarding through the Activity's `dispatchTouchEvent`. On timeout, the in-memory session is cleared and the UI returns to S02 without discarding any active scan session (NFR-20e) — the `RfidSessionEngine` continues running independently of the auth state, pausing only the UI.

### 6.4 RBAC Enforcement

A `RbacGuard` utility class is injected into all ViewModels:

```java
public class RbacGuard {
  private final UserSession session; // holds current role

  public void requireAdmin() throws UnauthorizedException { ... }
  public boolean isAdmin() { return session.getRole() == Role.ADMIN; }
}
```

UI elements for Admin-only actions are hidden (not just disabled) when role is OPERATOR (NFR-20k). ViewModels enforce the same check before executing any data-mutating operation.

---

## 7. Licensing Subsystem

### 7.1 Token Structure

```json
{
  "tokenId": "uuid-v4",
  "clientName": "Crazy Hamster Toy Store",
  "deviceId": "android-id-or-serial",
  "issueDate": "2026-06-01",
  "expiryDate": "2027-06-01"
}
```

Signed with vendor's RSA-2048 private key → Base64-encoded → delivered as `.lic` file or paste.

### 7.2 Verification Flow (On Every Launch)

```
1. Load token from private internal storage
2. Verify RSA signature using embedded public key (NFR-33)
        │
   Invalid sig → lock, "Invalid license"
        │
3. Verify deviceId == Build.getSerial() / Settings.Secure.ANDROID_ID (NFR-34)
        │
   Mismatch → lock, show expected vs actual device IDs
        │
4. Check expiry date vs device clock (NFR-35)
        │
   Expired → lock data-entry; allow read-only access to past sessions
   14 days remaining → warning banner
        │
5. Check tokenId against cached revocation list (NFR-37)
        │
   Revoked → lock all functionality, allow read-only
        │
6. Check revocation list freshness (NFR-37a)
        │
   Last fetch > 14 days ago AND no network → lock with "verification overdue"
        │
7. All checks passed → proceed to home screen
```

### 7.3 Revocation List Fetch

```
Background WorkManager task (periodic, every 7 days):
  GET {configured_revocation_url}
  Response: { "revokedTokens": [...], "updatedAt": "..." }
  On success: cache to private storage + update revocation_fetched_at
  On failure: no action (grace period applies)

On every API upload / LAN transfer attempt (NFR-37b):
  Force-fetch revocation list before executing the export
  If revoked → block export, lock app
```

WorkManager is preferred over AlarmManager for the periodic fetch — it respects battery optimization and works on AOSP without GMS (NFR-26), using the `UNMETERED` network constraint so it only runs on Wi-Fi.

---

## 8. Report Export & Delivery

### 8.1 CSV Generation

```
CsvEngine.generateInventoryReport(session, results, scope, fieldDefs)
  │
  ├── scope == FILTERED → use category-filtered lists
  └── scope == FULL     → use all results
  │
  Build rows:
    Available items: Status=Available, [fixed fields], [domain fields], RfidTagId
    Missing items:   Status=Missing,   [fixed fields], [domain fields], RfidTagId
    Excess items:    Status=Excess,    (blank product fields),           RfidTagId
  │
  Write to: {app_private_dir}/exports/{filename}.csv
  Filename: {InventoryCode_yyyyMMdd_HHmm}.csv  (OQ-08 default)
  Retained for 30 days (NFR-17, auto-purge via WorkManager)
```

### 8.2 Delivery Channels

```
┌─────────────────────────────────────────────────┐
│              DeliveryManager                    │
│                                                 │
│  deliver(file, channel, config): DeliveryResult │
│                                                 │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────┐│
│  │  ShareSheet  │ │  ApiUpload   │ │  LanSend ││
│  │  Deliverer   │ │  Deliverer   │ │  Deliver ││
│  └──────────────┘ └──────────────┘ └──────────┘│
└─────────────────────────────────────────────────┘

ShareSheet: FileProvider URI → Intent.ACTION_SEND → Android chooser
ApiUpload:  OkHttp multipart/form-data POST → configurable URL + token + headers
LanSend:    OkHttp POST to LAN URL (same mechanism, different endpoint)
            (SMB/FTP supported via optional third-party library if HTTP POST is insufficient)
```

Before any network delivery: force revocation list check (NFR-37b). All delivery attempts are recorded in `delivery_history` table (FR-47).

---

## 9. Backup & Restore

### 9.1 Three-Tier Implementation

**Tier 1 — Automatic Daily Local Backup (NFR-39–41)**

```
WorkManager PeriodicWorkRequest (daily, runs at midnight or first launch of day):
  1. VACUUM + checkpoint WAL on SQLite DB
  2. Copy DB file to: {SD_card or internal}/stockbuddy_backups/backup_{date}.db.gz
     (gzip compressed)
  3. Compute SHA-256 checksum → store alongside snapshot
  4. Delete snapshots older than 7 days (keep latest 7)
```

**Tier 2 — Manual Backup via Share Sheet (NFR-42–44)**

```
Admin taps "Backup Now" in Settings:
  1. Create temp dir in private storage
  2. Copy: SQLite DB + all files from /exports/ folder
  3. AES-256-GCM encrypt the ZIP
     Key derivation: HKDF(ANDROID_ID + VENDOR_SALT_embedded_in_APK)
  4. Write: stockbuddy_backup_{timestamp}.zip.enc
  5. Android share sheet → user routes to Google Drive / email / USB / etc.
```

**Tier 3 — Optional Nightly Remote Backup (NFR-45–46)**

```
WorkManager PeriodicWorkRequest (nightly, only if LAN/API endpoint configured AND Tier 3 enabled):
  1. Generate same encrypted ZIP as Tier 2
  2. HTTP POST to configured LAN/API endpoint
  3. On failure: retry up to 3× over 24 hours (exponential backoff)
  4. Persistent failure: non-blocking notification on next launch
```

### 9.2 Restore Flow

```
Admin selects .zip.enc file via file picker:
  1. Decrypt using same HKDF key (fails if different device without vendor key)
  2. Extract, validate SQLite checksum
  3. Show summary: creation date, record counts
  4. Admin confirms
  5. Close DB, replace DB files, reopen DB
  6. License token NOT overwritten (NFR-49)
  7. App remains functional immediately
```

---

## 10. Screen Navigation & State

### 10.1 Navigation Architecture

Single-Activity architecture using the Android Navigation Component (Jetpack Nav Graph). All screens (S01–S20) are Fragments navigated via NavController. Deep links are not required.

```
NavGraph:
  startDestination = first_run_guard (checks if setup complete)
        │
   first launch? → S19 (Setup Wizard) → S02 (PIN Login)
   subsequent?   → S02 (PIN Login)
        │
        └──► S03 (Home) — hub for all feature flows
```

### 10.2 First-Run Setup Wizard (S19) — State Machine

```
Step 1 (License Activation)
  → COMPLETE: license token valid, stored → proceed to Step 2
  → BACK: not allowed (license cannot be revisited once activated — FR-65c)

Step 2 (Admin Account Setup)
  → COMPLETE: admin account + operator PIN stored → proceed to Step 3
  → BACK: allowed, returns to Step 1 (display only — token already stored)

Step 3 (Domain Field Configuration)
  → COMPLETE: field definitions stored in field_config table → proceed to Step 4
  → BACK: allowed, returns to Step 2

Step 4 (Initial Category Setup)
  → COMPLETE: category list seeded → show summary screen (FR-67) → S02
  → BACK: allowed, returns to Step 3

All 4 steps complete → set setup_complete flag in app_config → never show S19 again
```

### 10.3 Scan Session State Persistence (Crash Recovery — NFR-07)

```
Session states stored in app_config:
  active_session_id    = null | UUID
  active_session_code  = null | String

On crash/restart:
  IF active_session_id != null:
    Show dialog: "You have an unfinished scan session. Resume or Discard?"
    Resume → load session tags from session_tags table → restore RfidSessionEngine
    Discard → delete session_tags, clear active_session_id
```

---

## 11. Non-Functional Requirements Mapping

| NFR | Requirement | Design Decision |
|-----|------------|-----------------|
| NFR-01 | Tag appears in count < 1s | `Flow<String>` emitted directly from Chainway SDK callback; UI subscribed via `collectLatest` on main dispatcher |
| NFR-02 | Results in < 3s for 10K items | In-memory `HashMap` for O(1) RFID lookup; set subtraction for Missing; runs on `Dispatchers.IO` |
| NFR-03 | CSV import 5K rows < 3s | Batch DB inserts via Room `insertAll()` in a single transaction; CSV parsed line-by-line |
| NFR-04 | CSV export 10K rows < 2s | `BufferedWriter` to disk; no intermediate List — stream-write each row |
| NFR-05 | Cold start < 2s | Room DB pre-warmed via `openHelperFactory`; minimal work on main thread at startup |
| NFR-06 | 500+ tags/min | Chainway SDK handles RF protocol; dedup via `HashSet.add()` (O(1)); no DB write per tag |
| NFR-07 | Crash recovery | Periodic flush of `session_tags` (every 50 tags); active session ID in `app_config` |
| NFR-15 | Local only | No network calls in any Repository; `NetworkInterceptor` in OkHttp asserts user-initiated flag |
| NFR-16 | 500 MB storage budget | `StorageStatsManager` checked at launch; warning banner if > 80% |
| NFR-25 | Battery < 15%/hr | Chainway SDK power settings; no background RFID polling; WorkManager battery-friendly for backups |
| NFR-26 | No GMS | WorkManager with `DefaultWorkerFactory`; no Firebase, no Google Maps, no Play Services APIs |
| NFR-27 | Layered architecture | UI → ViewModel → Repository → Room (enforced via module `:core:db` visibility) |
| NFR-28 | Java | All source files in `.java`; Kotlin stdlib excluded from dependencies |

---

## 12. Trade-off Analysis

### 12.1 Local SQLite vs. Embedded Server DB (e.g. Realm, ObjectBox)

**Decision: SQLite via Room**

| Aspect | SQLite/Room | Alternative (Realm/ObjectBox) |
|--------|------------|-------------------------------|
| Android ecosystem fit | Native, zero extra libs | Extra dependency, binary size |
| JSON column support | SQLite JSON1 (built-in) | Requires workaround |
| Full-text search | FTS5 available | Varies |
| AOSP compatibility | Guaranteed | Varies |
| Team familiarity | High (standard Android) | Medium |

Room wins on all axes for this deployment profile.

### 12.2 In-Memory Dedup vs. DB-Backed Dedup During Scan

**Decision: In-memory `HashSet` per session**

Pros: O(1) lookup, zero I/O latency, meets NFR-01.
Cons: Session lost on crash before periodic flush.
Mitigation: Flush to `session_tags` every 50 tags (crash buffer). Acceptable tradeoff — losing at most 50 tags in a crash is recoverable by re-scanning.

### 12.3 WorkManager vs. AlarmManager for Periodic Tasks

**Decision: WorkManager**

AlarmManager would work on AOSP but provides no retry logic, no constraint support (network, battery), and no persistence across reboots out of the box. WorkManager provides all of these and works on AOSP without GMS.

### 12.4 PBKDF2 vs. bcrypt for Credential Hashing

**Decision: PBKDF2 with HMAC-SHA256**

bcrypt is not natively available on Android without a third-party library. PBKDF2 with 100,000 iterations is available via `javax.crypto` (standard on Android 9+) and meets OWASP recommendations. Avoids an extra dependency and GMS-free concern.

### 12.5 Single-Activity vs. Multi-Activity Navigation

**Decision: Single-Activity + Nav Component**

The shared ViewModel scope (NavGraph-scoped ViewModels) makes it straightforward to pass scan session state between Inventory screens (S11→S12→S13) without serialization. Multi-Activity would require Parcelable everywhere or a global singleton.

### 12.6 AES-256-GCM for Backup Encryption vs. ZIP password

**Decision: AES-256-GCM with HKDF-derived key**

ZIP password protection (ZipCrypto) is weak and broken. AES-256-GCM is authenticated encryption — it detects tampering. Key derived from `ANDROID_ID + vendor salt` means the backup is device-bound by default, matching the license's device-binding model. Restoring to a different device requires the vendor salt (embedded in APK) to match, which it will for all Gigakin-distributed APKs — consistent behavior across franchise stores.

---

## 13. Open Items Impact on Design

| OQ | Impact if assumption is wrong |
|----|------------------------------|
| OQ-01 (QR schema) | If not JSON, `FieldConfigEngine.parseFromQR()` must be updated; all other components unaffected |
| OQ-02 (Article ID source) | If from RFID EPC, the Linking form removes the Article ID field and derives it from the tag read; DB schema unchanged |
| OQ-10 (API upload spec) | OkHttp request builder in `ApiUploadDeliverer` must match authentication scheme; interface unchanged |
| OQ-11 (LAN protocol) | If SMB required instead of HTTP POST, add a third-party SMB library (e.g. JCIFS-NG); `LanSendDeliverer` implementation changes, interface unchanged |
| OQ-30 (Article ID uniqueness) | If Article IDs are NOT unique per unit, change grouping logic in `ResultsComputationEngine` to group by Barcode only; RFID Tag ID becomes the unit-level key in expanded group cards (FR-33 note already covers this) |

---

## 14. What to Revisit as the System Grows

1. **Multi-device sync**: If Crazy Hamster expands to multiple C72 devices per store, the local-only model becomes a constraint. A lightweight sync protocol (CRDTs or a simple last-write-wins API) would be needed. The layered Repository design makes this feasible — add a `RemoteDataSource` alongside the existing `LocalDataSource` without touching ViewModels.

2. **Cloud portal**: If head office wants a web dashboard aggregating CSV data from all franchise stores, the current API upload channel is sufficient in the short term. Long term, a structured REST API replacing the raw CSV POST would be more maintainable.

3. **Article ID auto-generation**: If OQ-30 resolves that Article IDs are absent from labels, the app will auto-generate them. Consider whether this auto-ID needs to be globally unique across franchise stores — if yes, a UUID-based scheme rather than a sequential counter.

4. **Performance ceiling**: At 10,000 items (NFR-02), in-memory computation is fast. Above ~50,000 items, consider moving result computation to a SQL query (EXISTS subquery instead of full table scan in memory).

5. **Security questions recovery**: The proposed set (OQ-28) is weak against social engineering. If the Admin is the store owner, consider adding a TOTP-based recovery as an alternative in a future version.

6. ~~**Audit log growth**~~ — **Resolved.** NFR-20o added: 90-day configurable retention window; daily `AuditLogPurgeWorker` deletes entries older than the cutoff. See §4.2.1 for detail.
