# StockBuddy — MVP Demo Test Plan (Physical C72)

**Purpose:** Validate the MVP end-to-end on a real Chainway C72, with emphasis on the hardware-dependent
paths that could not be verified on the emulator (RFID scan, 2D imager for barcode/QR, and the live
reader-status bar). Scoped to a small demo dataset of **5 physical RFID tags**.

**Audience:** whoever runs the demo/acceptance pass. Tick each box; note failures with the observed vs.
expected. FR/NFR references point back to `docs/srs/RFID_App_SRS_C72.md`.

---

## ⚠️ 0. Build & install for the device (read first)

The **device build must NOT use `-Pemu`.** That flag exists only for the x86_64 AOSP emulator and
**excludes every native `.so`** (`app/build.gradle.kts` → `jniLibs.excludes += "**/*.so"`). The real
C72 needs the ARM libs (`libDeviceAPIM.so` etc.), so build the plain variant:

```bash
# From StockBuddy/ — DEVICE build (includes ARM native libs):
./gradlew clean assembleDebug            #  ✅  no -Pemu
#   → app/build/outputs/apk/debug/app-debug.apk

# Install (device connected via USB, developer mode on):
adb devices                              # confirm the C72 is listed
adb install -r app/build/outputs/apk/debug/app-debug.apk
#   (or Android Studio → Run ▸ select the C72)
```

- [ ] **0.1** Built **without** `-Pemu`; APK installed on the C72.
- [ ] **0.2** App launches without crashing; Home shows the 4 tiles.
- [ ] **0.3** On launch the reader status bar reads **"Reader Connected"** in **green** (not grey).
  *(Grey "No RFID Reader" here means the SDK didn't initialise — likely a `-Pemu` build or a
  permissions/hardware issue. Stop and resolve before continuing.)*

---

## 1. Recommended demo dataset (5 tags)

To exercise **Available / Missing / Excess** *and* category scoping with only 5 tags, link **4** and
leave **1 unregistered**:

| Label | EPC (fill in your tag's) | Linked to | Product | Barcode | Category |
|---|---|---|---|---|---|
| T1 | `__________` | ✅ | Rugged Phone | `1111111111116` | Electronics |
| T2 | `__________` | ✅ | Rugged Phone | `1111111111116` | Electronics |
| T3 | `__________` | ✅ | Scanner Grip | `2222222222229` | Accessories |
| T4 | `__________` | ✅ | Scanner Grip | `2222222222229` | Accessories |
| T5 | `__________` | ❌ **unlinked** | — (acts as the "unregistered" tag) | — | — |

This gives: two products, **grouping** (2 units each), **two categories** (for the filter/scoping demo),
and a spare tag to demonstrate **Excess**. Adjust names/barcodes freely; keep the 4-linked + 1-unlinked
shape. Physically **label the tags T1–T5** so you can remove specific ones during scan scenarios.

**Also prepare:**
- [ ] One **printed/screen QR code** encoding the JSON payload (for the QR-linking test) — see §4.3.
- [ ] One **printed 1D barcode** (e.g. an EAN-13) to scan into the Barcode field — §4.4. `2222222222229`
      or any sample from `docs/srs/StockBuddy_Test_Values_RFID_Barcode.md`.
- [ ] A way to **power-cycle / disconnect** the UHF module (for the live-disconnect test, §2.2).
- [ ] Device charged; USB debugging available for logcat if something misbehaves.

> **Demo limits are not in play** at this scale (caps are 200 items / 50 categories / 100 sessions).
> No limit testing needed for the demo.

---

## 2. Reader status & hardware sanity (FR-81 / FR-81a) — *device-only, never seen on emulator*

- [ ] **2.1** Status bar green **CONNECTED** on Home and on every screen (Assets, Settings, Linking, …).
- [ ] **2.2** **Live disconnect:** with the app open, power-cycle / disconnect the reader. The bar
      turns **filled-red "Reader Not Connected"** *without* navigating away, then back to **green** on
      reconnect. *(Validates the SDK connection callback wired in `ChainwayScannerManager`.)*
- [ ] **2.3** **FR-81a:** while disconnected, the RFID **Scan** button (Individual Linking) and the
      Inventory **START** button are **disabled** with a clear inline message; non-hardware features
      (Categories, Field Config, Bulk CSV, Settings, browsing results) stay usable.

---

## 3. Setup: Field Configuration & Categories

- [ ] **3.1 Field Config** (Settings → Field Configuration): apply a template and/or add a custom
      field, e.g. **Price (Number)**. Confirm it now appears on Individual Linking, Assets, and CSV.
- [ ] **3.2 Categories** (Settings → Category Management): add **Electronics** and **Accessories**.
      Try adding a **duplicate** name → rejected with feedback. Rename works; delete warns.

---

## 4. Linking — all three paths (FR-05–19)

### 4.1 Individual (manual + RFID scan)
- [ ] **4.1a** Link **T1** to *Rugged Phone*: type Name/Barcode, pick **Electronics**, tap the **RFID
      Scan** button and read **T1** → EPC fills in. Fill the mandatory custom field. **Save** → centered
      "Item linked successfully".
- [ ] **4.1b** Repeat for **T2** → same product/barcode (proves multi-unit grouping under one barcode).
- [ ] **4.1c Validation:** try to link **T2 again** → **duplicate RFID** rejected. Try an unknown
      category → rejected ("add it in Settings first"). Leave a mandatory field blank → inline error.

### 4.2 Bulk (CSV)
- [ ] **4.2a** Bulk Linking → **Download Template** → saved to Downloads.
- [ ] **4.2b** Fill rows for **T3, T4** (Product *Scanner Grip*, Accessories) and import → summary shows
      **Inserted 2**. Re-import the same file → **Skipped 2** (append-only). A bad category row → **Rejected**.

### 4.3 QR Code Linking (S07) — *device imager only*
Prepare a QR encoding this JSON (see `util/QrPayload.kt`; `attributes` keys are **field keys**, e.g. `price`):
```json
{ "productName": "Rugged Phone", "barcode": "1111111111116",
  "category": "Electronics", "rfid": "", "attributes": { "price": "199.00" } }
```
- [ ] **4.3a** Linking Options → **QR Code Linking** → screen shows the aim-zone; scan the QR with the
      **imager** → routed to **Individual Linking pre-filled** from the payload.
- [ ] **4.3b** Complete RFID (scan a real tag if `rfid` was blank), **Save** → returns to the **QR
      scanner** re-armed (FR-11). **Back** from the pre-filled form also returns to the scanner.
- [ ] **4.3c** Scan a **malformed / non-JSON** QR → inline "unrecognized" message, scanner re-arms.

### 4.4 Barcode field scan (FR-06) — *device imager only*
- [ ] **4.4** In Individual Linking, tap the **barcode** end-icon and scan a real 1D barcode → the
      Barcode field populates from the imager.

*(After §4, you should have 4 tags linked across 2 products/2 categories; T5 remains unlinked.)*

---

## 5. Assets (S09)

- [ ] **5.1** Assets lists **2 products** with correct unit counts (2 each) and custom fields.
- [ ] **5.2** Expand **"RFID Tags (N)"** on a card → individual tag EPCs listed.
- [ ] **5.3** **Delete** one tag (confirm dialog) → count drops; **Edit/Reassign** a tag to the other
      product (searchable picker) → centered "Item reassigned", it moves cards.
- [ ] **5.4** **Search** by name, by barcode, and by **RFID** (type part of an EPC) → matching product
      surfaces and its tag list **auto-expands** with the matched tag highlighted.
- [ ] **5.5** **Category filter** narrows the list. **Edit product** (name/category/custom field) saves.
- [ ] **5.6** **Delete a product** → warns about linked units, then cascades.

*(If you delete/reassign here, re-link so you're back to 4 linked tags before §6.)*

---

## 6. Inventory scan — the core (FR-29–47)

For each scenario: Inventory → enter a code → (pick category) → **START** → present the relevant tags to
the antenna → watch the **live unique count** and the **Available / Missing / Excess** cards → **STOP** →
review Results.

- [ ] **6.1 All present, category "All":** lay out **T1–T4 + T5**, scan. Expect **Available 4, Missing 0,
      Excess 1** (T5 is the unregistered tag). Live count reaches 5.
- [ ] **6.2 Missing:** remove **T4**, scan **T1–T3 + T5**. Expect **Available 3, Missing 1, Excess 1**.
- [ ] **6.3 Category-scoped (headline behaviour, FR-35/46):** START a session scoped to **Electronics**.
      Scan everything. The **live cards** count Available/Missing only for **Electronics** (T1/T2);
      **Excess** (T5) still shows. On the Scanning screen the category is a **locked label**, not a dropdown.
- [ ] **6.4 Results after a scoped session:** STOP → Results defaults to **Electronics**, and the category
      control is a **read-only label** (no dropdown), because switching to another category would be 0/0.
- [ ] **6.5 Results after an "All" session:** the category filter **is** an interactive dropdown; changing
      it re-filters live (no re-scan). Excess always retained.
- [ ] **6.6 Grouping & tabs:** Results groups units by barcode with counts; Available/Missing/Excess tabs
      populate; tap a group to expand its RFID units.
- [ ] **6.7 Snapshot immutability (FR-46):** note the Results numbers. Go to Assets, **edit a product name**,
      reopen the **same session from Reports** → the historical result is **unchanged** (reads the snapshot).

---

## 7. Export & Reports

- [ ] **7.1 CSV — scoped export:** from a **category-scoped** session's Results, **Download** the CSV →
      it contains **only that category's** Available/Missing rows **+ all Excess** (matches on-screen).
- [ ] **7.2 CSV — column order & custom fields:** header is `Status, Name, Barcode, Category, [domain
      fields], RFID Tag ID`; the custom field (Price) is present.
- [ ] **7.3 Share:** the **Share** channel opens the Android chooser with the CSV attached.
- [ ] **7.4 Reports list:** past sessions listed newest-first with code + timestamp; opening one shows its
      snapshot; delete asks to confirm.

---

## 8. Explicitly OUT of scope (do not test — not built for MVP)

Licensing / activation • Authentication / RBAC / login • Backup & Restore • First-run wizard •
API-upload / LAN-transfer export channels • Google Play Services of any kind. If any of these appear or
are expected, that's a spec mismatch, not a test failure.

---

## 9. Device-only checklist (things the emulator could never show)

Confirm each was actually exercised on the C72 this pass:

- [ ] UHF **single-tag** scan (Individual Linking RFID button)
- [ ] UHF **continuous** scan (Inventory session live count)
- [ ] 2D imager **QR** decode (S07)
- [ ] 2D imager **1D barcode** decode (Barcode field)
- [ ] Reader status **green CONNECTED** + **filled-red NOT_CONNECTED** + **live disconnect/reconnect**

---

## 10. 5-minute "golden path" (script for the live audience)

A clean happy-path narrative once the above passes:

1. Launch → **green Reader Connected**.
2. Settings → show **Field Config template switch** (the "reconfigure in 30s" pitch) → add **Price**.
3. Add categories **Electronics / Accessories**.
4. **Link** T1+T2 to *Rugged Phone* (RFID scan), T3+T4 to *Scanner Grip* (or bulk CSV). Show one **QR link**.
5. **Assets** → expand tags, show search-by-RFID auto-expand.
6. **Inventory** → scan all 5 → **4 Available / 1 Excess**; remove one → **Missing** appears live.
7. **STOP** → Results grouped by product; **Export CSV** → open it.
8. Reopen the session from **Reports** to show the frozen snapshot.

---

### Failure logging

| # | Step | Expected | Observed | Sev | Notes |
|---|------|----------|----------|-----|-------|
|   |      |          |          |     |       |
