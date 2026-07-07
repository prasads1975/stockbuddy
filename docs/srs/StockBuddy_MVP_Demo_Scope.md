# StockBuddy — MVP / Demo Build Scope

**Based on:** RFID App SRS C72, v3.8
**Purpose:** A buildable, demo-first slice of the full SRS — optimized for showing the same app to stakeholders across different retail domains in a single live session, without the enterprise hardening (licensing, backup, RBAC) that a real customer rollout would eventually need.

**Demo narrative this scope is built to support:** *configure the fields and categories for this customer's industry, link a few items live, run a stock-take, filter the results by category, export the report* — all without touching code, in under 5 minutes, repeatable for any vertical.

---

## 1. In-Scope Requirements by Module

### 1.1 Domain Field Configuration — *the headline feature*
| FR | Requirement |
|----|-------------|
| FR-77i | Always-on fixed fields (Name, Barcode, Category, RFID Tag ID) shown as a reference list |
| FR-77j | Admin defines zero or more domain-specific fields (key, label, type, mandatory, visibility) |
| FR-77l | Starter templates (e.g. "Toy Retail: Price", "Jewellery: Net/Gross Weight, Purity") — **the live domain-switch demo moment** |
| FR-77m | Admin may proceed with zero domain-specific fields |
| FR-77n | Field definitions persisted as metadata |
| ~~FR-77o~~ | ~~Article ID usage toggle~~ — **dropped (v2).** Article ID is no longer a fixed field or a mode toggle; barcode is the sole business key. If needed, Article ID is added as an ordinary configurable domain field (FR-77j) stored in the product's `attributes`. |
| FR-80a | Standalone Settings → Field Configuration screen, usable anytime (not gated behind a setup wizard) |
| NFR-51–54 | Configurable fields, dynamic rendering across Linking/Assets/Inventory/CSV, null-handling, persisted storage |
| ~~NFR-56~~ | ~~Article ID configurability~~ — superseded by the drop above; covered by the general configurable-field mechanism (NFR-51–54). |

*Build note: implement the domain-field configuration (FR-77i–n) as a standalone screen reached directly from Settings — skip the non-skippable wizard framing (FR-77 itself) and the dropped Article ID toggle entirely.*

### 1.2 Category Management
| FR | Requirement |
|----|-------------|
| FR-12, FR-12a | Category master list; dropdown validation on the Linking form. Category is an **FK** on products (v2) — linking/import reject an unknown category ("add it in Settings first"), no auto-create |
| FR-72 | Category Management screen accessible from Settings |
| FR-73 | Add / edit / delete a category |
| FR-74 | Delete a referenced category → **block + warn** with impact count; on confirm, cascade-delete its products and their linked units (v2) |

### 1.3 Individual Linking (manual entry only)
| FR | Requirement |
|----|-------------|
| FR-05 | Manual entry form: fixed fields (Name, Barcode, Category, RFID) + domain-specific fields |
| FR-06 | Barcode: type or imager scan |
| FR-07, FR-07a | RFID Tag ID: type or UHF "Scan" button (single-tag enforcement) |
| FR-08 | RFID uniqueness validation |
| FR-09, FR-09a, FR-09b | Mandatory fixed fields + mandatory domain-specific fields + asterisk UI |
| ~~FR-09c~~ | ~~Article ID validation, mode-dependent~~ — dropped (v2); no Article ID field |
| FR-10 | Save locally, immediately usable in scans |
| FR-21 | Product written first on save (insert-or-reject-on-name-mismatch); no dedicated Product Master screen |

*UX note: the mode-switching concern (QR Scan vs. Manual Entry) is moot for this scope — QR mode (FR-01–04) is already cut, so there's only ever one form on screen, with Barcode and RFID Tag ID each carrying their own inline scan affordance (NFR-10f). Barcode scan is a compact inline icon next to the field; RFID Tag ID scan is a more prominent labelled "Scan" button, given it's the field the whole app's uniqueness logic hangs on and it has richer feedback states (success / no tag / multiple tags). Save Link stays the one true oversized, thumb-zone primary action (NFR-10c/d) — distinct from both scan buttons.*

### 1.4 Bulk Linking
| FR | Requirement |
|----|-------------|
| FR-15, FR-16 | Bulk Linking screen; CSV column schema (dynamic domain columns) |
| FR-17 | **Append-only** import (v2): product insert-or-reject-on-name-mismatch; unit insert if RFID new, else skip |
| FR-18 | Import result summary — **Inserted / Skipped / Rejected** with per-row reasons |
| FR-19 | Reject blank/duplicate-RFID/unknown-category rows, continue processing the rest |

### 1.5 Inventory Session
| FR | Requirement |
|----|-------------|
| FR-29 | Enter Inventory Code before starting a session |
| FR-30, FR-31 | Past sessions list, reverse chronological (lets you re-show prior demo runs) |
| FR-33 | START button |
| FR-34 | Real-time unique-tag count |
| FR-35 | Category filter dropdown (display/reporting only, doesn't affect scanning) |
| FR-36 | STOP button |
| FR-37 | Real-time dedup |

### 1.6 Results Summary
| FR | Requirement |
|----|-------------|
| FR-40, FR-41, FR-42 | Available / Missing / **Excess** definitions |
| FR-43, FR-48 | Available, Missing, **and Excess** tabs |
| FR-44 | Item cards show fixed fields + domain-specific fields |
| FR-45 | Grouped by Barcode with per-group count **per tab** (same barcode can appear in Available and Missing with different counts); tap to expand the individual RFID units in that status |
| FR-46 | Category filter carries over from scan screen, re-appliable without re-scanning |
| FR-47 | Session name + STOP timestamp shown |

*v2 note: results are read from the immutable `session_result_items` snapshot written at STOP (System Design §4.0.5), so a reopened historical session shows what products looked like at scan time — it does not drift when master data is later edited.*

*Build note: simplify FR-39's KPI card to a single count row reflecting the active filter, rather than the full dual-row (unfiltered + filtered) version.*

### 1.7 Report Export
| FR | Requirement |
|----|-------------|
| FR-49 | Export icon — **Share and Download only**, skip the API/LAN channel choices |
| FR-51 | CSV filename from Inventory Code + timestamp |
| FR-52 | Android share sheet delivery |
| FR-52a | Download (save to device): location picker, save confirmation with file path, option to chain into Share |

### 1.8 Assets (product-level, v2)
| FR | Requirement |
|----|-------------|
| FR-61 | Flat list of all **products** (one row per barcode) with a linked-unit count — this is the product UI (supersedes a separate Product Master screen) |
| FR-62 | Product cards show name, barcode, category, unit count + domain-specific fields |
| FR-63 | Search bar (name / barcode / category) |
| FR-64 | Total count, updates live with search/filter |
| FR-65, FR-66 | **Edit** a product (name, category, attributes — barcode is the PK, read-only); **delete** a product → block + warn, then cascade-delete its linked units |
| FR-67 | Category filter |

### 1.9 Settings (minimal shell)
A single Settings tile with exactly two destinations: **Categories** (§1.2) and **Field Configuration** (§1.1). No login, no role gating, no License/Backup/Admin Password/Operator PIN items.

### 1.10 Hardware / Scanner Connection Status
| FR | Requirement |
|----|-------------|
| FR-81 | Persistent status indicator with three states: **Connected**, **Not Connected**, **Not Available** (covers non-C72 devices and emulators) |
| FR-81a | Graceful degradation — RFID/imager-dependent actions (Scan buttons, START) disable with a clear inline message when the reader isn't available, rather than failing silently or crashing. All non-hardware features (Field Config, Categories, Bulk Linking, results browsing, Settings) stay fully usable regardless |

*This directly serves the emulator-based testing workflow below — the indicator is what lets anyone using the app immediately tell whether they're looking at real scanner behavior or running without hardware.*

### 1.11 Non-Functional Requirements

Most NFR sections aren't all-or-nothing — some line items are demo-critical (they're literally what makes the live demo feel snappy and professional) while others are data-volume or lifecycle concerns that only matter once there's a real device fleet. Picked selectively:

**Performance — keep what makes the live demo feel responsive:**
| NFR | Requirement |
|-----|-------------|
| NFR-01 | Real-time tag count updates within 1 second of detection |
| NFR-06 | Scan engine handles 500+ tags/min without dropping reads or freezing the UI |

*Skip:* NFR-02–04 (results/import/export performance at 5,000–10,000 row scale) and NFR-05 (cold start) — demo datasets are a fraction of that size; revisit before a real pilot.

**Reliability:**
| NFR | Requirement |
|-----|-------------|
| NFR-08 | Data survives device restarts without corruption |
| NFR-09 | Core workflows (link, scan, summary, Download/Share export) work fully offline — this is a genuine selling point, not just an engineering nicety |

*Skip:* NFR-07 (crash/session recovery) — a short demo session is unlikely to hit this; defer.

**Usability — directly affects how polished the demo looks, and how well it survives being handled on the actual rugged device:**
| NFR | Requirement |
|-----|-------------|
| NFR-10 | Minimum 48×48dp touch targets |
| NFR-10a | Material Design 3 as the design system — not custom/ad-hoc UI |
| NFR-10b | Layouts designed and verified against a 6-inch-class portrait display (the C72's actual screen size) |
| NFR-10c | Primary action per screen (Scan, START, STOP, Save Link) gets an oversized touch target — 56dp minimum, full/near-full width |
| NFR-10d | Primary action positioned in the lower half of the screen — thumb-reachable for one-handed operation, not in the header |
| NFR-10e | One primary action + minimum supporting info per screen; everything else deferred to a secondary screen/modal |
| NFR-10f | Barcode/RFID scan buttons on Individual Linking are field-level affordances (exempt from NFR-10e) — Barcode gets a compact inline icon, RFID gets a more prominent labelled button; Save Link remains the one true primary action |
| NFR-11 | Consistent Available (green) / Missing (red) / Excess (yellow) colour coding |
| NFR-12 | Key flows reachable within 3 taps from home |
| NFR-13 | Plain-language error messages — no raw stack traces in front of a customer |
| NFR-13a | Success/error/confirmation messages clearly visible (size, contrast, positioning) under showroom lighting — not colour-only, not obscured by keyboard/hand, critical errors persist until acknowledged |

*Skip:* NFR-14 (20-minute training-time target) — an outcome to validate later with real users, not something to build against.

*Open question carried from the SRS (OQ-31):* does the C72 unit have a physical hardware trigger button for scan/imager activation? If so, it likely becomes the primary one-handed scan mechanism in practice, and the on-screen button (NFR-10c/d) serves more as a fallback and accessibility path than the primary interaction — worth confirming before finalizing the scanning-screen layout.

**Storage & Data:**
| NFR | Requirement |
|-----|-------------|
| NFR-15 | Local-only storage — no automatic remote transmission (simplified: drop the revocation-list carve-out since Licensing is cut) |
| NFR-18 | RFID Tag ID uniqueness enforced at the database layer |

*Skip:* NFR-16 (storage budget), NFR-17 (30-day CSV retention), NFR-19a/19b (session retention purge, storage warnings) — all data-lifecycle management that's irrelevant for a dataset that lives for one demo session.

**Hardware Compatibility — non-negotiable, you're building on the real device regardless of scope:**
| NFR | Requirement |
|-----|-------------|
| NFR-21 | Chainway C72 is the certified target |
| NFR-22 | Android 9 (API 28)+ |
| NFR-23 | Chainway official UHF SDK |
| NFR-24 | C72 imager for barcode scanning (manual-entry Barcode field, FR-06) |
| NFR-26 | No Google Play Services dependency (AOSP) |
| NFR-26a | Runs without crashing on an Android emulator / any device lacking the RFID SDK — hardware-dependent features degrade per FR-81a instead of failing |

*Skip:* NFR-25 (battery drain target) — worth knowing, not something to gate a demo build on.

**Maintainability — cheap to set up right on day one, expensive to retrofit later:**
| NFR | Requirement |
|-----|-------------|
| NFR-27 | Layered architecture (UI → ViewModel → Repository → Room DB) |
| NFR-28 | Java (Chainway SDK integration uses its native Java APIs directly) |

*Skip:* NFR-29 (debug log export), NFR-30 (APK sideload updates) — operational conveniences, not demo-blocking.

**Cut entirely (whole sections):** 5.5 Authentication/RBAC (NFR-20a–o), 5.8 Licensing (NFR-31–38), 5.9 Backup & Restore (NFR-39–50) — consistent with the FR cuts in Section 2 below.

### 1.12 Architecture & Scalability Principles (build right, don't optimize)

Section 1.10 deferred *verifying* performance at scale (NFR-02–04: 10,000-item results computation, 5,000-row CSV import) — correctly, since none of that can be meaningfully tested on a 50-item demo dataset. But there's a separate question from verification: **does the underlying design paint you into a corner?** A handful of decisions cost the same amount of code today whether or not the demo ever exercises them, but are real rework to retrofit once the UI and data layer are built around the easy path. These aren't testable NFRs — they're build-time discipline:

- **Index RFID Tag ID and Barcode from the first DB migration.** RFID uniqueness is already required (NFR-18); Barcode needs the same treatment since it's the grouping/matching key for every results computation and Product Master upsert (Section 1.7).
- **Keep the hybrid SQLite-columns + JSON-attributes data model exactly as specified in Section 1.7.** This is already the scalable choice — it's the reason adding a domain-specific field for a new customer never requires a schema migration or app update. Don't simplify it away for the demo; it's no more work to build correctly than to hardcode fields.
- **Use the Repository pattern (NFR-27) so Available/Missing/Excess computation is one swappable code path**, not logic duplicated across the scanning screen, results screen, and export.
- **Write the Assets list and Inventory Reports list queries as paginated/lazy-loaded from day one**, even though the 50-item cap never forces it. It's the same Room/SQLite code either way — retrofitting pagination onto a UI built around "load everything into a list" is the kind of rework worth avoiding now.
- **Avoid full-table scans in results computation (FR-40/41/45).** Index-backed joins on RFID/Barcode, not in-memory filtering of every record — same effort to write either way, very different behavior once item counts grow past demo size.

What still stays deferred, unchanged from Section 2: load testing, query tuning against real 10k+ datasets, caching layers, DB sharding/partitioning, and storage-budget enforcement. None of that is buildable discipline — it's verification work that only makes sense once there's a real dataset to verify against.

---

## 2. Explicitly Out of Scope for MVP/Demo

| Area | FRs / Sections | Why cut |
|------|----------------|---------|
| Licensing | NFR-31–38, Section 5.8, FR-77a–d, FR-80 | No real device fleet to license yet |
| Backup & Restore | NFR-39–50, Section 5.9 | No production data at risk yet |
| Authentication / RBAC | NFR-20a–o, Section 5.5, FR-77e–h, screen S02 | Login screens slow down a live demo |
| QR Scan linking mode | FR-01–04, FR-11 | Requires a label-printing dependency; manual entry suffices for live demo |
| Standalone Product Master screen | FR-22–28 | The product-level **Assets** screen (v2, §1.8) is the product UI — list/edit/delete; no separate screen needed |
| API / LAN export channels | FR-53–56 | No real endpoint/server to demo against |
| Non-skippable First-Run Wizard | FR-77, FR-77a–h, FR-78, FR-78a, FR-79 | Replaced by direct Settings access to Field Config + Categories |
| Delivery history, re-export, dual-CSV export | FR-57–60 | Polish features, not demo-critical |
| Assets sort / detail view | FR-68 | Stretch goal (edit/delete FR-65/66 are now **in scope** — see §1.8) |
| Category bulk upload, operator read-only | FR-75, 76 | Only Admin will touch this in a demo (delete-warning FR-74 is now **in scope** — see §1.2) |
| Session delete | FR-32 | Not needed for a short-lived demo dataset |
| Performance/storage at scale, session retention, crash recovery, training-time target | NFR-02–05, 07, 14, 16, 17, 19a–b, 25, 29, 30 | Data-volume and lifecycle concerns — revisit before a real pilot, not demo-blocking |

---

## 3. Demo Script

```
1. Settings → Field Configuration → pick a template for this customer's domain
   (fields adapt: Price, or Net/Gross Weight + Purity, or Batch No/Expiry, etc.)
   → set Article ID usage (Not Used / Optional / Mandatory) to match their labeling reality
2. Settings → Categories → show/edit categories for this customer's shelves
3. Link 4–5 items live (manual entry), picking categories and filling domain fields
4. Bulk Linking → import a pre-built CSV to seed ~40 items instantly (leaves headroom under the 50-item demo cap for the live-linked items in step 3)
5. Inventory → enter a session code → START → walk the floor → STOP
6. Results: filter by category → Available/Missing counts and list re-filter instantly
7. Switch filter to "All" → full picture; tap a grouped card to see individual units
8. Export → choose Share (email/WhatsApp the CSV) or Download (save to device, useful when there's no signal on the showroom floor)
9. Assets → flat list, search, category filter — "this is everything on the device"
```

---

## 4. Demo Mode Limits

A demo device that's been used for a dozen customer visits shouldn't visibly groan under the weight of stale data — and it shouldn't be possible to mistake it for a production deployment either. These caps are a **build-time configuration for the demo app only** (e.g. a `BuildConfig` constant) — they don't belong in the customer-facing SRS, since a real pilot deployment would have these limits removed or raised. Worth flagging to whoever builds this so the values live in one place and are trivial to change or strip out later.

These are `BuildConfig` constants in `app/build.gradle.kts` (`DEMO_MAX_ITEMS` / `DEMO_MAX_CATEGORIES` / `DEMO_MAX_SESSIONS`), read via `DemoLimits.kt`. **Current values: 200 / 50 / 100** (raised from the original 50 / 10 / 25 to support fuller demo datasets).

| Resource | Cap | Behaviour at cap | Enforced at |
|----------|-----|-------------------|-------------|
| **Linked items (Assets)** | 200 | Manual Linking save and Bulk Linking import are blocked once the cap is reached. Inline message: *"Demo limit reached (200 items). Delete an item to add a new one."* A Bulk Linking CSV that would exceed the cap imports rows up to the remaining headroom, then rejects the rest with reason "Demo item limit reached" in the import summary. | Individual Linking save; Bulk Linking import |
| **Categories** | 50 | Adding a category past the cap is blocked from the Category Management screen. Message: *"Demo limit reached (50 categories). Edit or delete an existing category to add a new one."* | Category Management add |
| **Inventory sessions** | 100 | **Rolling retention, not a hard block** — when the cap is exceeded, the oldest existing session is automatically purged to keep the 100 most recent. This keeps a demo device usable indefinitely across many customer visits. | Inventory session creation |

A demo with a 5-minute live walkthrough (per the script in Section 3) never gets close to these numbers in a single sitting — they exist to stop *cumulative* buildup across repeated demos on the same device, not to constrain any single demo run.

---

## 5. Open Decision for You

Field Configuration and Category Management are reachable from Settings with no login gate in this scope — meaning **anyone with the device can reconfigure the domain on the spot**, which is exactly what you want for a sales demo (you control the device) but would need an Admin gate before any real customer pilot. Worth deciding now whether that gate gets added in the next phase or stays open through the pilot too.

---

## 6. Testing Strategy: Emulator vs Physical Device

Most of this build is verifiable without ever touching a C72. Splitting test scope this way keeps day-to-day development fast while still requiring real-hardware sign-off before anything ships:

| Test on **Android emulator** | Test on **physical C72** only |
|---|---|
| Field Configuration + starter templates (§1.1) | UHF RFID scan (Scan button in Linking, START/STOP in Inventory) — real tag reads |
| Category Management (§1.2) | Barcode imager scan (§1.3, FR-06) |
| Individual Linking — manual entry, validation, mandatory-field UI (§1.3) | Reader connection status transitions in real conditions (power-cycle, range, interference) |
| Bulk Linking — CSV import, validation, result summary (§1.4) | Battery drain under active scanning (NFR-25) |
| Results Summary — tabs, grouping, category filter (§1.6) | |
| Export — Share and Download (§1.7) | |
| Assets — list, search, category filter (§1.8) | |
| Demo Mode Limits behavior (§4) | |

This split only works because of **FR-81 / FR-81a / NFR-26a** (§1.10–1.11): the app must detect whether real RFID/imager hardware is present and degrade gracefully rather than crash, so the emulator path is actually usable rather than just "the parts that happen not to call the SDK." Concretely:

- On the emulator, the status indicator should show **Not Available**, and the Scan/START buttons should be disabled with the inline message rather than throwing an SDK exception.
- Everyone running the app — developer, tester, or you during a demo — should be able to glance at the status indicator and immediately know whether they're looking at real scanner behavior or not. No silent guessing.
- Before any customer-facing demo or release, run a short physical-device pass covering just the right-hand column above. That's the only part the emulator can't tell you anything about.

---

## 7. Build & Tooling Decisions

These are delivery-process decisions, not product requirements — they live here rather than in the SRS (the language strategy is the one exception, since it's now formalized as NFR-28).

**Language:** Kotlin (primary) + Java (Chainway SDK integration, and any other Java-only third-party library). This is the standard modern Android pairing — Kotlin and Java are fully bidirectionally interoperable, so the SDK is called directly from Kotlin with no wrapper layer needed.

**Codebase:** From scratch. No existing reference app source code — the jewellery-client reference app mentioned in the SRS was only used for UI/UX screenshots during requirements gathering.

**Package name:** `com.gigakin.stockbuddy`

**Branding:** Logo and exact brand color palette to follow later. Until then, build against MD3's default semantic colors (with Available/Missing/Excess as green/red/yellow per NFR-11) so the structure is ready to re-skin without rework.

**Chainway SDK integration — general steps:**
1. Chainway typically ships the SDK as one or more `.aar` files (and possibly supporting `.jar`/native `.so` libraries for the UHF module). Place these in the app module's `libs/` folder.
2. In `build.gradle` (app module), point Gradle at that folder:
   ```gradle
   repositories {
       flatDir { dirs 'libs' }
   }
   dependencies {
       implementation(name: 'chainway-sdk', ext: 'aar') // match actual filename
   }
   ```
3. Check the SDK's own documentation/sample app (Chainway usually ships one) for:
   - Required `AndroidManifest.xml` permissions (UHF/imager modules on the C72 are typically accessed over an internal serial/USB bus, not Bluetooth — permissions vary by SDK version)
   - Minimum SDK version compatibility (cross-check against NFR-22's Android 9/API 28 floor)
   - Any required native library ABI filters (`armeabi-v7a` / `arm64-v8a`) in `build.gradle`'s `ndk { abiFilters ... }` block
4. Wrap all SDK calls behind the Repository layer (NFR-27), not called directly from UI code — this is also what makes the emulator-mode graceful degradation (FR-81a) clean to implement: the repository can detect "SDK present and responding" vs. "not available" in one place, rather than every screen needing its own try/catch.

*I haven't seen the actual SDK package contents, so step 2–3 above are general Android-SDK-integration practice rather than Chainway-specific instructions. Once you share the SDK files/docs, I can give exact dependency declarations and manifest entries instead of the general pattern.*

**Physical device access:** Available on a need basis during development, guaranteed available before release. This is sufficient — per Section 6, the emulator covers everything except actual scan/imager hardware and connection-state behavior, so physical-device time only needs to be scheduled around those specific verification passes, not continuously.

**Signing & Distribution — explained:**

These are two separate things that both need a decision before the first build goes onto more than one device:

- **Signing** is a cryptographic requirement, not optional — every Android APK must be signed before it can install on any device. During development, Android Studio auto-generates a throwaway "debug" key for you automatically; that's fine for your own emulator/test device, but it's not meant for anything you'll reinstall over time or hand to someone else. For a demo app installed on multiple devices across multiple customer visits, you want **one proper release keystore** (a private key file + password) used consistently for every build — that's what lets a new build install *over* an old one instead of requiring an uninstall, and keeps the app's identity consistent. The keystore file and its passwords are sensitive: if lost, you can never update that app installation again without uninstalling it first, so it needs to live somewhere secure (a password manager or secrets store, not committed to the Git repo) with one clear owner.
- **Distribution** is just "how does the built APK file get onto each device." Given the no-CI/CD, on-demand-device situation right now, the simplest options are:
  - **ADB sideload** — plug the device into a laptop via USB, run `adb install app-release.apk`. Fine for one or two devices you have physical access to.
  - **Manual file transfer** — copy the APK via USB drive, Drive link, or similar, and tap "Install" on the device (requires enabling "install unknown apps" once per device). Reasonable if devices are occasionally out of your hands.
  - *(Not recommended yet: cloud distribution tools like Firebase App Distribution typically require Google Play Services to be present on the receiving device to install through them — which conflicts with NFR-26's AOSP-only constraint on the C72. Worth avoiding until/unless that constraint is revisited.)*

**Recommendation for this phase:** generate one release keystore now (quick, low-effort) even though distribution stays manual — it costs nothing today and avoids a painful "every device needs a fresh uninstall" problem the first time you need to push an update to a unit already out in the field.

**CSV library:** Default (e.g. OpenCSV or Apache Commons CSV — both pure-Java, no Google Play Services dependency, consistent with NFR-26).

**CI/CD:** None for this phase — manual builds. Revisit once there's a real release cadence to justify the setup cost.

**Test data:** No seed CSVs will be created in advance — you'll build those at testing time. A separate reference sheet of realistic-format **sample RFID Tag ID and Barcode values** (for manual entry during emulator-based testing, before physical tags are in hand) is provided separately.
