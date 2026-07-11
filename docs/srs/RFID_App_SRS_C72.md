# Software Requirements Specification
## StockBuddy — RFID Inventory Management Android Application
### Deployed on Chainway C72 Handheld RFID Reader

**Application Name:** StockBuddy
**Client:** Crazy Hamster Toy Store — Yavatmal, Maharashtra
**Website:** https://www.crazyhamster.in
**Document Version:** 3.17 (Draft)
**Date:** July 2026

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Stakeholders and User Roles](#2-stakeholders-and-user-roles)
3. [Application Modules Overview](#3-application-modules-overview)
4. [Functional Requirements](#4-functional-requirements)
5. [Non-Functional Requirements](#5-non-functional-requirements)
6. [Design Constraints and Guidelines](#6-design-constraints-and-guidelines)
7. [Open Items and Questions](#7-open-items-and-questions)
8. [Document Revision History](#8-document-revision-history)

---

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification (SRS) defines the functional and non-functional requirements for **StockBuddy** — an Android-based RFID inventory management application deployed on the Chainway C72 handheld RFID reader device. StockBuddy is a single-brand product deployable across any retail client. This instance documents the deployment for Crazy Hamster Toy Store, Yavatmal. The application enables store staff to register products with RFID tags, conduct RFID-based stock-take sessions, generate inventory reports, and deliver those reports to a remote system — either via an API upload over the internet or via file transfer over a local area network (LAN).

### 1.2 Scope

The application is strictly scoped to the following five capabilities:

1. **Stock Entry (Linking)** — Associate an RFID tag to a toy product's master record, either individually (form + QR/barcode scan) or in bulk via CSV import.
2. **Stock Taking — Open Scan** — Create a named inventory session and scan all RFID tags present in the store to build a stock snapshot.
3. **Stock Taking — Close / Summary** — Stop the scan session and display a summary of Available, Missing, and Excess items, grouped by Barcode (SKU), with Article ID shown where configured for the deployment (Section 1.7).
4. **Report Viewing** — Browse past inventory sessions and inspect item-level detail for Available and Missing items.
5. **Report Export & Send** — Export a session's results as a CSV file and deliver it via one of four channels:
   - **Download** — save the CSV directly to local device storage, with a folder/location picker
   - **Android share sheet** — send via any installed app (email, WhatsApp, Google Drive, etc.)
   - **API upload** — HTTP POST the CSV to a configured remote endpoint over the internet
   - **LAN transfer** — push the CSV to a server or shared folder on the local network (e.g. store's Wi-Fi)

**Explicitly out of scope for this version:**
- A persistent cloud or remote database that stores inventory records centrally — all master data and sessions remain on device.
- Stock return / send-back workflow.
- SKU auto-deletion or lifecycle management.
- Multi-device synchronisation or real-time data sharing between devices.
- Web management portal.
- Point-of-sale or billing integration.

### 1.3 Definitions and Abbreviations

| Term | Type | Definition |
|------|------|------------|
| StockBuddy | Product | The RFID inventory management Android application documented in this SRS |
| RFID | Acronym | Radio Frequency Identification — each physical toy unit carries a UHF RFID tag that uniquely identifies it |
| Article ID | Field | **Dropped as a fixed field (v3.15).** Formerly an optional deployment-configurable per-unit identifier; now, if wanted, just a configurable domain-specific field (Section 1.7). **RFID Tag ID** is the guaranteed unique per-unit identifier; **Barcode** is the product-level business key. |
| Barcode | Field | Product-level barcode printed on the packaging; shared across all units of the same product variant. The primary product-level key used for grouping and Product Master matching (Section 1.7). |
| SKU | Acronym | Stock Keeping Unit — used interchangeably with **Barcode** at the product variant level. Article ID, where configured, identifies a specific physical unit within a SKU. |
| Category | Field | Fixed field on every item; product category whose value list is configurable per deployment (e.g. for toy retail: Ride-On, Board Game, Educational, Magic Car, Hot Wheels, Craft Kit) |
| Domain-Specific Field | Field | A metadata-driven attribute defined per deployment for a given industry/client (e.g. Price for toy retail; Net Weight, Gross Weight, Purity for jewellery) — see Section 1.7 |
| Linking | Feature | The process of associating an RFID tag to a toy item's master record on the device |
| Individual Linking | Feature | Linking one item at a time using a form with QR / barcode scan assist |
| Bulk Linking | Feature | Importing multiple item–RFID associations in one step via a CSV file |
| Inventory Session | Feature | A named stock-take event: the user opens a session, scans with RFID, then stops and reviews results |
| Inventory Code | Field | User-defined name/label for an inventory session (e.g. "3 June Morning", "Shelf A Count") |
| Available | Status | Item whose RFID was detected during the scan AND has a matching record in the local master — stock confirmed present |
| Missing | Status | Item that exists in the local master but whose RFID was NOT detected during the scan |
| Excess | Status | RFID tag detected during scan that does NOT match any record in the local master |
| C72 | Device | Chainway C72 Android-based handheld UHF RFID reader |
| CSV | Acronym | Comma-Separated Values — file format used for bulk import and report export |
| QR Code | Term | 2D matrix barcode scannable via the C72 imager; used during Individual Linking to auto-populate item fields from warehouse-generated labels |

### 1.4 Document Conventions

- Requirements are identified as **FR-XX** (Functional) or **NFR-XX** (Non-Functional).
- Priority: **P1** = Must Have, **P2** = Should Have, **P3** = Nice to Have.
- All timestamps use the device local time zone (IST).

### 1.5 Business Context

Crazy Hamster is a toy retail store and franchise chain headquartered in Yavatmal, Maharashtra. The product catalogue spans ride-on vehicles, board games, educational toys, Hot Wheels, magic cars, art & craft kits, and more — ranging from low-unit-price impulse buys to high-value ride-on items. Inventory accuracy is a key operational challenge given the breadth of SKUs and the physical footprint of a toy showroom. This application addresses the stock-take workflow specifically.

### 1.6 Assumptions and Constraints

- All data (master records, inventory sessions, exported reports) is stored **locally on the C72 device only**. No data is transmitted to any remote system automatically.
- Each RFID tag is globally unique across all items in the store. A Barcode/SKU may appear on multiple physical units of the same product.
- RFID tags are pre-attached to toy packaging before the linking step. Linking is performed at the store on the C72.
- The C72 has a built-in UHF RFID antenna and a 2D imager for QR/barcode scanning, accessible via Chainway's Android SDK.
- The app is used in a retail showroom environment — adequate lighting, no glove requirement, but the device may be handled one-handed while moving through shelves.
- No network connectivity is required for any core workflow. Connectivity is only used when the user explicitly shares/exports a report.
- The QR code on warehouse-generated product labels encodes at minimum: Barcode, Product Name, RFID Tag ID, any configured domain-specific fields (Section 1.7), and Article ID where the deployment uses it.

### 1.7 Data Model Approach: Fixed and Domain-Specific Fields

StockBuddy is a single-brand product deployable across multiple retail industries (Section 1.1). While the core RFID linking, scanning, and reporting workflows are industry-agnostic, the **descriptive attributes** captured for an item differ by domain (e.g. a toy retailer needs Price; a jewellery retailer needs Net Weight, Gross Weight, and Purity). To avoid requiring application code changes — and a corresponding app update — for each new deployment or evolving requirement, the set of domain-specific item fields shall be configurable via metadata.

**Always-on fixed fields** — present in every deployment regardless of industry, stored as indexed database columns, and referenced throughout this SRS:
- RFID Tag ID
- Barcode
- Product Name
- Category

**Article ID — DROPPED as a fixed field (v3.15, superseded).**
> **v3.15 update:** The Article ID *fixed field* and its 3-mode toggle (Not Used / Optional / Mandatory) described below are **no longer part of the design and were never implemented.** Barcode is the sole product-level business key and RFID Tag ID the sole per-unit uniqueness key. If a deployment wants an "Article ID"-style identifier, it is defined as an **ordinary configurable domain-specific field** (see below / NFR-51–54) and stored in the product's `attributes` JSON — it carries no key/uniqueness/grouping role. Accordingly, **FR-77o, NFR-56, FR-09c, FR-65o, and the Article-ID mode control in FR-80a are void**, and every "Article ID (where enabled)" clause throughout this SRS (e.g. FR-02, FR-05, FR-16, FR-23, screens S06/S13/S14) resolves to *not present as a fixed field*. See `docs/design/StockBuddy_System_Design.md` §4.0. The original text is retained below, struck through, for history.

~~**Article ID — optional fixed field (deployment-configurable):** Unlike the four fields above, not every industry or client pre-prints a unique, per-physical-unit identifier on their product labels. Article ID is therefore treated as a **fixed field whose presence is configured per deployment**, with three modes:~~
- ~~**Not Used** — Article ID is not captured, not displayed, and does not appear on the Linking form, Bulk Linking CSV schema, Assets list/detail, Inventory results, or report CSV export.~~
- ~~**Optional** — Article ID is captured and displayed where applicable, but is not required to save a record.~~
- ~~**Mandatory** — Article ID is required to save a record, validated the same way as the other fixed mandatory fields (FR-09).~~

**RFID Tag ID remains the guaranteed unique identifier for a physical unit** (per the Section 1.6 assumption that every RFID tag is globally unique), and **Barcode remains the key used for product-level grouping and Product Master matching** (FR-21, FR-22, FR-45). The app's core workflows (linking, scanning, grouping, matching) do not depend on Article ID at all. This also resolves the risk previously tracked as OQ-30.

**Domain-specific fields** — defined per deployment via configuration, stored as a JSON attribute set per item:
- Examples: Price, Net Weight, Gross Weight, Purity, or any other attribute relevant to a given industry.
- Each field definition specifies: field key, display label, data type (text, number, dropdown, date), whether the field is mandatory, and where it appears (Linking form, Asset list/detail, Inventory summary, CSV import/export).

Throughout Sections 4 and 5, any FR/NFR that lists item fields (e.g. on the Linking form, Asset list, Inventory results, or CSV columns) should be read as: **the always-on fixed fields above, plus Article ID if enabled for the deployment, plus whichever domain-specific fields are configured for the current deployment.**

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-51 | Configurable fields | The Superadmin shall be able to define, add, edit, or remove domain-specific item fields at install time or at runtime, without requiring an application code change or APK update. |
| NFR-52 | Dynamic rendering | The Individual Linking form (manual entry and QR-prefill), Bulk Linking CSV column mapping, Assets List/Detail, and Inventory Summary item cards shall render domain-specific fields dynamically based on the current field configuration, in addition to the fixed fields listed above. |
| NFR-53 | Existing record handling | When a new domain-specific field is added after items already exist, existing item records shall have a `null` value for that field until edited. The app shall not require or perform a bulk backfill. |
| NFR-54 | Field configuration storage | Field definitions (metadata) shall be stored locally on-device (e.g. as a JSON configuration record in the local database) and shall persist across app updates and be included in backup/restore (Section 5.9). |
| NFR-55 | Category configuration unaffected | Category remains a fixed field in all deployments; only the list of valid Category *values* is configurable per deployment (Section 4.5, Screen S18), which is a data-configuration concern separate from field-schema metadata. |
| ~~NFR-56~~ | ~~Article ID configurability~~ | **VOID (v3.15).** The Article ID fixed field and its Not Used / Optional / Mandatory toggle are dropped. An "Article ID" identifier, if a deployment wants one, is just a configurable domain-specific field (NFR-51–54). |

**Data storage approach**: Always-on fixed fields and system fields (internal record ID, linked timestamp, session references) are stored as standard indexed SQLite columns for efficient lookup during scanning and reporting. Article ID, when enabled for the deployment, is also stored as an indexed column (nullable) rather than in the JSON attribute set, since it may still be used for search/filter even though it is not used for matching or grouping. Domain-specific fields are stored in a single JSON column per item record (e.g. `attributes`), using SQLite's built-in JSON1 functions (`json_extract`, `json_set`, `json_each`) for read/write and filtering. This hybrid approach preserves query performance on RFID/Barcode lookups — which occur on every scan — while keeping domain-specific attributes fully flexible.

---

## 2. Stakeholders and User Roles

| Role | In-App Account | Responsibilities |
|------|---------------|-----------------|
| **Admin** | One per device — individual named account with password | Full access: manage master data, categories, users, settings, backup/restore, license, export reports |
| **Operator** | Shared PIN — all floor staff use the same credential | Operational access: link items via QR scan, run inventory sessions, view results, export reports |

> Since the app is fully local with no backend, there is no remote administrator or multi-device user management in this version. All user accounts are managed on-device by the Admin.

---

## 3. Application Modules Overview

The app exposes three top-level modules from the home screen:

| Module | Description |
|--------|-------------|
| **Linking** | Master data entry — comprises three sub-functions: (1) Individual Linking via QR scan or manual entry, (2) Bulk Linking via CSV import, and (3) Product Master Management (auto-upsert on scan/save, bulk CSV upload, view/edit). |
| **Inventory** | Stock-take sessions — create a named session, scan via UHF RFID, stop, and view Available / Missing / Excess summary and item-level detail. |
| **Assets** | View all linked items currently in the local database — a flat searchable list of every RFID-tagged physical unit registered on the device. Supports individual item deletion (Admin only). |
| **Settings** | App configuration: License status/renewal, Category Management, Admin password, Operator PIN, idle timeout, Backup & Restore, App Info (Section 4.5). |

---

## 4. Functional Requirements

### 4.1 Linking — Master Data Entry

The Linking module handles two distinct but related concerns: (a) **item linking** — associating each physical item's RFID tag to its product record, and (b) **product master management** — maintaining the catalogue of product definitions (the fixed fields Product Name, Barcode, Category, plus Article ID where enabled for the deployment, plus any configured domain-specific fields per Section 1.7) that item records are built from. The product master can be populated automatically via QR scan, pre-loaded via CSV upload, or managed manually — all three paths are supported.

#### 4.1.1 Individual Linking

The Individual Linking screen supports two entry modes. Both modes lead to the same form, the same validations, and the same **Save Link** action:

- **QR Scan mode** — a single QR code scan via the C72 imager auto-populates all fields at once. This is the primary and fastest path when items arrive with warehouse/supplier-generated QR labels.
- **Manual Entry mode** — the user fills each field individually. The Barcode field supports typing or imager scan. The RFID Tag ID field supports typing or UHF scan via a dedicated Scan button. This is the fallback when a QR label is absent, damaged, or not yet in use.

The eye icon (top-right) on this screen navigates to the Assets list.

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-01 | P1 | The Individual Linking screen shall offer two clearly labelled entry modes: **"Scan QR"** (activates the C72 imager to scan a single QR code that populates all fields) and **"Enter Manually"** (displays the form for field-by-field entry). Both modes shall be accessible from the same screen. |
| FR-02 | P1 | **QR Scan mode:** On a successful QR scan, the system shall parse the payload and auto-populate all item fields: the fixed fields (**Product Name, Barcode, Category**), Article ID (where enabled for the deployment, per Section 1.7), plus any configured domain-specific fields (e.g. Price), and **RFID Tag ID**. No further scanning steps are required. |
| FR-03 | P1 | After QR auto-population, the system shall display all parsed field values for the user to review and correct before saving. |
| FR-04 | P1 | If the QR payload cannot be fully parsed (corrupted code, missing mandatory fields), the system shall identify the missing fields and switch to manual entry mode pre-populated with whatever was successfully parsed, so the user can complete the remaining fields manually. |
| FR-05 | P1 | **Manual Entry mode:** The screen shall display input fields for the fixed fields (**Product Name, Barcode, Category (dropdown)**), Article ID (where enabled for the deployment, per Section 1.7), any configured domain-specific fields for the current deployment (e.g. Price), and **RFID Tag ID**. |
| FR-06 | P1 | The **Barcode** field in manual entry mode shall support two sub-modes: (a) manual keyboard entry, and (b) imager scan via the C72 barcode scanner — activated by a scan icon or button adjacent to the field. |
| FR-07 | P1 | The **RFID Tag ID** field in manual entry mode shall support two sub-modes: (a) manual keyboard entry of the full EPC hex string, and (b) UHF scan via a **"Scan"** button that activates the C72 UHF reader and captures the next tag read into the field. |
| FR-07a | P1 | **Single-tag enforcement for UHF scan in Individual Linking:** When the **"Scan"** button is activated, the system shall perform a single-shot UHF inventory burst at the minimum supported transmit power (to limit read range to the immediately adjacent tag). The result shall be handled as follows: **(a) Exactly one tag detected** — populate the RFID Tag ID field with the detected EPC and return focus to the form; **(b) No tags detected** — display an inline error *"No tag detected. Hold the device close to the tag and try again."* — the field is not modified; **(c) Two or more tags detected** — display an inline error *"Multiple tags in range (N detected). Remove other tags from the scanning area and try again."* — the field is not modified and no tag is auto-selected. The transmit power shall be restored to the session default after the scan burst completes, regardless of outcome. |
| FR-08 | P1 | On save (both modes), the system shall validate that the RFID Tag ID is unique across all existing records. A duplicate RFID shall be rejected with a clear error identifying the conflicting record. |
| FR-09 | P1 | On save (both modes), the system shall validate that all always-on fixed mandatory fields — **Product Name, Barcode, Category, and RFID Tag ID** — are non-empty before persisting the record. Any field that is empty shall be highlighted with an inline error identifying which field(s) are missing. The record shall not be saved until all mandatory fixed fields are present. |
| FR-09a | P1 | In addition to the fixed mandatory fields (FR-09), the system shall validate all configured domain-specific fields that have been marked as **mandatory** (`mandatory: true`) in the active field configuration (Section 1.7). If any mandatory domain-specific field is empty at save time, the system shall display an inline error for each such field and prevent saving until they are all populated. This validation applies to both QR Scan mode and Manual Entry mode. |
| FR-09b | P1 | **Mandatory field indication on UI:** All mandatory fields — both the five fixed mandatory fields (FR-09) and any domain-specific fields configured as mandatory (FR-09a) — shall be visually distinguished on the Individual Linking form (S06) by displaying an asterisk (**\***) after the field label (e.g. *"Product Name \*"*). A single legend line below the form title shall read *"\* Required field"* so the convention is clear to the user. This indication shall be rendered at form load time, before any save attempt. |
| FR-09c | P1 | **Article ID validation (mode-dependent):** Save-time handling of Article ID shall follow the deployment's configured mode (Section 1.7, NFR-56): **(a) Not Used** — the field is not shown and no validation applies; **(b) Optional** — the field may be left empty; if populated, it is saved as entered with no uniqueness check; **(c) Mandatory** — the field shall be validated as non-empty using the same inline-error pattern as FR-09. Article ID, when populated, is never used as a uniqueness key for save validation — RFID Tag ID uniqueness (FR-08) is the sole per-unit uniqueness check. |
| FR-10 | P1 | The saved record shall be stored locally on device and immediately available for use in inventory scan sessions. |
| FR-11 | P2 | After a successful save in QR Scan mode, the system shall automatically re-arm the QR scanner so the user can scan the next item without navigating away — supporting rapid sequential linking. |
| FR-12 | P2 | The system shall maintain a **Category master list** on the device (seeded with defaults: Ride-On, Board Game, Educational, Magic Car, Hot Wheels, Craft Kit, Soft Toy, Action Figure, Outdoor, Other). Individual entries can be added or removed by the Admin from the settings screen. |
| FR-12a | P2 | The Category value from either entry mode shall be validated against the Category master list. If the value does not match any existing entry, the system shall alert the user — *"Category '[value]' is not in the system list"* — and offer two options: **(a)** add the new value to the Category master list and proceed, or **(b)** manually select an existing Category before saving. |
| FR-12b | P2 | The system shall support **bulk upload of the Category master list** via a CSV file containing a single column: **Category Name**. On upload, the system shall upsert the list — adding new entries and skipping existing ones. |
| FR-13 | P2 | The eye icon on the Individual Linking screen shall navigate to the Assets screen (Section 4.4), which lists all currently linked item records. |
| FR-14 | P2 | The system shall allow editing an existing linked item record from the Assets screen, with all fields editable. RFID Tag ID edits shall be subject to the same uniqueness validation as a new save. |

#### 4.1.2 Bulk Linking (CSV Import)

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-15 | P1 | The system shall provide a Bulk Linking screen that accepts a CSV file from the device file system. |
| FR-16 | P1 | The expected CSV column schema shall be: the fixed fields (**Name, Barcode, Category, RFID**), an **Article ID** column (included only if enabled for the deployment, per Section 1.7), plus any configured domain-specific fields (e.g. Price). The system shall validate that the selected file contains at minimum the mandatory columns (Name, Barcode, RFID, and Article ID only if the deployment's Article ID mode is Mandatory) before importing. |
| FR-17 | P1 | On import, the system shall upsert records: insert rows where the RFID does not yet exist; update product fields (Name, Barcode, Category, and any configured domain-specific fields) where the RFID already exists. |
| FR-18 | P1 | The system shall display an import result summary: number of records inserted, updated, and rejected, with reasons for each rejection (e.g. missing RFID, duplicate RFID within the file). |
| FR-19 | P2 | The system shall reject any CSV row where the RFID field is blank or duplicated within the import file itself, and continue processing the remaining valid rows. |
| FR-20 | P2 | The system shall display a preview of the first five rows of the selected CSV before the user confirms the import. |

#### 4.1.3 Product Master Management

The Product Master is the catalogue of product definitions on the device. Each product record holds the SKU-level attributes shared across all physical units of the same product: the fixed fields (Product Name, Barcode, Category), Article ID where enabled for the deployment, plus any configured domain-specific fields (e.g. Price). It is a distinct entity from linked items — one product record can have zero or many linked physical units (RFID tags) associated with it.

The Product Master can be populated via three paths, all of which coexist:

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-21 | P1 | **Auto-upsert on save:** When an item is saved during Individual Linking (via either QR scan or manual entry), the system shall automatically upsert the Product Master — inserting a new product record if the **Barcode** does not yet exist, or updating the Name, Category, Article ID (if enabled), and any configured domain-specific fields if the Barcode already exists. This ensures the Product Master is always in sync with linked items without any manual setup. |
| FR-22 | P1 | **Bulk CSV upload:** The system shall support uploading a Product Master CSV file with columns: the fixed fields (**Product Name, Barcode, Category**), an Article ID column (included only if enabled for the deployment), plus any configured domain-specific fields (e.g. Price). On upload, the system shall upsert the product catalogue — inserting new products and updating existing ones matched by **Barcode**. This allows head office or warehouse to pre-load the full product catalogue onto the device before physical stock arrives. |
| FR-23 | P1 | The system shall validate the Product Master CSV before import: **Product Name and Barcode are mandatory**; rows missing either shall be rejected with a clear reason. Article ID is mandatory only if the deployment's Article ID mode (Section 1.7, NFR-56) is set to Mandatory. Category values shall be validated against the Category master list (same rule as FR-12a). |
| FR-24 | P1 | The system shall display a Product Master import result summary: records inserted, updated, and rejected with reasons. |
| FR-25 | P2 | **Product Master view:** The system shall provide a Product Master screen listing all product records, searchable and filterable by Product Name, Barcode, Category, or Article ID (where enabled for the deployment). Each row shall show the count of linked physical units (RFID tags) associated with that product. |
| FR-26 | P2 | The system shall allow editing an existing product record (Name, Category, Article ID if enabled, and any configured domain-specific fields) directly from the Product Master screen. Edits shall propagate to all linked item records sharing that **Barcode**. |
| FR-27 | P2 | The system shall allow deleting a product record from the Product Master, with a confirmation prompt. If linked item records exist for that product, the system shall warn the user and require explicit confirmation before proceeding with deletion of the product and all its linked items. |
| FR-28 | P3 | The system shall allow exporting the current Product Master as a CSV file via the Android share sheet, for backup or sharing with head office. |

---

### 4.2 Inventory — Stock Taking

An inventory session is a named stock-take event. The user enters an Inventory Code, scans the store's RFID tags, stops the scan, and the app computes Available / Missing / Excess counts against the local master.

#### 4.2.1 Session Management

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-29 | P1 | The system shall prompt the user to enter an **Inventory Code** (free-text session name, e.g. "3 June Morning") before starting a new scan session. |
| FR-30 | P1 | The system shall maintain a list of all past inventory sessions on an **Inventory Reports** screen, each showing Inventory Code and creation timestamp. |
| FR-31 | P1 | Past sessions shall be listed in reverse chronological order (most recent first). |
| FR-32 | P2 | The system shall allow the user to delete a past session from the list, with a confirmation prompt. |

#### 4.2.2 Scanning (Open)

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-33 | P1 | The Inventory Scanning screen shall display a prominent **START** button. Pressing START begins continuous UHF RFID scanning via the C72 antenna. |
| FR-34 | P1 | During scanning, the screen shall display a real-time running count of unique RFID tags detected since START was pressed. |
| FR-35 | P1 | The screen shall provide a **Category filter dropdown** ("All" or a specific category). The category filter is a **display and reporting filter only** — it has no effect on the RFID scanning itself. The C72 antenna always scans and collects all RFID tags in range regardless of the selected category. Filtering is applied after STOP when computing and displaying results. |
| FR-36 | P1 | The screen shall provide a **STOP** button (visually distinct — e.g. red, full-width) to end the scan session. |
| FR-37 | P1 | The system shall deduplicate RFID tags in real time: if the same tag is read multiple times during a session it shall be counted only once. |
| FR-38 | P2 | The session shall remain active until the user explicitly presses STOP, even if the device is momentarily moved away from the shelves. |

#### 4.2.3 Results Summary (Close)

After the user presses STOP, the system computes and immediately displays the session results.

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-39 | P1 | The results screen shall display a **Summary card** with two rows of KPIs: **(a) Full session (unfiltered):** Total in Master, Total Scanned (unique tags); **(b) Filtered view (category-scoped):** Available count (green chip), Missing count (red chip), Excess count (yellow chip). When "All" is selected, both rows show the same scope. When a specific category is active, the filtered KPIs reflect only items belonging to that category. |
| FR-40 | P1 | **Available** = RFID tags that were scanned AND have a matching record in the local master. |
| FR-41 | P1 | **Missing** = Records in the local master whose RFID was NOT detected during the scan. |
| FR-42 | P1 | **Excess** = RFID tags detected during scan that do NOT match any record in the local master (unregistered units). |
| FR-43 | P1 | The results screen shall provide at minimum two tabs: **Available** and **Missing**, listing the corresponding items. |
| FR-44 | P1 | Each item card in the results list shall display the fixed fields (**Product Name, Barcode, Category**), Article ID where enabled for the deployment, along with any configured domain-specific fields for the current deployment. |
| FR-45 | P1 | The results list shall be **grouped by Barcode** so that multiple physical units of the same product are visually clustered, with a per-group count shown. Tapping a grouped card shall expand to show the individual linked units within that group, each displaying its **RFID Tag ID** (and Article ID, where enabled for the deployment) and status, for Missing/Excess groups, navigable to the full detail view (FR-68). **RFID Tag ID is always the unit-level identifier used for grouping and expansion logic**, since it is guaranteed unique per the Section 1.6 assumption; Article ID, where present, is shown as supplementary information only and never required for this logic to function. *(This resolves the risk previously tracked as OQ-30.)* |
| FR-46 | P2 | The category filter selected on the scanning screen shall carry over to the results screen. The user shall be able to change the category filter on the results screen at any time without re-scanning — the system shall re-apply the filter to the stored raw tag set immediately. The underlying raw scan data (all tags collected) shall always be preserved in full regardless of which filter is active. |
| FR-47 | P2 | The results screen shall show the session name (Inventory Code) and the timestamp when STOP was pressed. |
| FR-48 | P2 | An **Excess** tab shall list unrecognised RFID tag IDs detected during the scan, to help staff identify unregistered or mislabelled items. |

---

### 4.3 Report Export & Send

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-49 | P1 | The results screen shall include a **share/export icon** (e.g. top-right corner). Tapping it shall present the user with a choice of delivery channel: **Share** (Android share sheet), **Download** (save to local device storage), **Upload via API**, or **Send via LAN**. |
| FR-50 | P1 | On confirming export, if a category filter is active the system shall ask the user: **"Export filtered ([Category] only) or full session (all categories)?"** The generated CSV shall reflect the chosen scope, with columns: **Status**, the fixed fields (**Product Name, Barcode, Category**), Article ID (included only if enabled for the deployment), any configured domain-specific fields, and **RFID Tag ID**. When no filter is active ("All"), the full session is exported without the prompt. |
| FR-51 | P1 | The generated CSV file shall be named using the Inventory Code and timestamp (e.g. `3_June_Morning_20260603_1111.csv`). |
| FR-52 | P1 | **Share sheet delivery:** The system shall invoke the Android share sheet so the user can send the CSV via any installed app (email, WhatsApp, Google Drive, local file manager, etc.). |
| FR-52a | P1 | **Download (save to device) delivery:** The system shall support saving the generated CSV directly to local device storage, with a folder/location picker (consistent with the reference app's original save behaviour). On completion, the system shall display a confirmation showing the saved file path/name, with an option to open the containing folder or immediately invoke the share sheet (FR-52) on the saved file. |
| FR-53 | P1 | **API upload:** The system shall support HTTP POST upload of the generated CSV to a configurable remote endpoint URL. The endpoint URL, authentication token (if required), and any custom headers shall be configurable in app settings. |
| FR-54 | P1 | The system shall display the API upload result to the user: success (HTTP 2xx) with a confirmation message, or failure with the HTTP status code and an option to retry. |
| FR-55 | P1 | **LAN transfer:** The system shall support transferring the generated CSV to a destination on the local network. Supported mechanisms (confirm preference — see OQ-10): SMB shared folder path, FTP server, or HTTP POST to a LAN server. The destination address and credentials shall be configurable in app settings. |
| FR-56 | P1 | The system shall display the LAN transfer result to the user: success with confirmation, or failure with the error reason and a retry option. |
| FR-57 | P2 | The system shall retain previously generated CSV files in a dedicated local folder on device for at least 30 days. |
| FR-58 | P2 | Past session reports shall be re-exportable from the Inventory Reports list screen without re-running a scan, using any of the three delivery channels. |
| FR-59 | P2 | The system shall record the delivery history for each session report: channel used, destination, timestamp, and outcome (success / failure). |
| FR-60 | P3 | The system shall support exporting Available and Missing lists as **two separate CSV files** in a single export action. |

---

### 4.4 Assets

The Assets module is a top-level home screen module that provides a flat, searchable list of all linked items (physical units) currently registered in the local database. Each entry represents one RFID-tagged physical unit. It gives store staff and the Admin a quick way to view what is currently in the system without running an inventory session.

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-61 | P1 | The Assets screen shall display a flat list of all linked item records currently in the local database, one card per physical unit. |
| FR-62 | P1 | Each item card shall display: **Product Name, Barcode, and RFID Tag ID** (full EPC hex string), plus Article ID where enabled for the deployment. |
| FR-63 | P1 | The Assets screen shall provide a **search bar** allowing the user to filter the list in real time by Product Name, Barcode, RFID Tag ID, or Article ID (where enabled for the deployment). |
| FR-64 | P1 | The Assets screen shall display the **total count** of linked items currently in the database, updated in real time as search filters are applied. |
| FR-65 | P1 | The Assets screen shall provide a **delete** action (Admin only). Tapping the delete icon shall present a confirmation prompt before removing the selected linked item record. Deletion removes only the linked item record — the corresponding Product Master record is not affected. |
| FR-66 | P2 | The Assets list shall be sorted alphabetically by Product Name by default. The user shall be able to re-sort by Barcode, by Article ID (where enabled for the deployment), or by most recently added. |
| FR-67 | P2 | The Assets screen shall support filtering by Category, allowing the user to view all linked units belonging to a specific product category. |
| FR-68 | P2 | Tapping an item card shall open a detail view showing all fields for that linked item: the fixed fields (**Product Name, Barcode, Category**), Article ID where enabled for the deployment, any configured domain-specific fields, **RFID Tag ID**, and the date it was linked. |

---

### 4.5 Settings, Setup, and Administration

This section defines the Settings module (a new tile on the Home screen, Admin-visible items gated per RBAC, Section 5.5), the Category Management screen, and the consolidated first-run setup flow covering license activation and Admin account creation.

#### 4.5.1 Settings Screen

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-69 | P1 | The Home screen shall include a **Settings** tile, accessible to both Admin and Operator roles. |
| FR-70 | P1 | The Settings screen shall present a menu of items. Items visible to **all roles**: App Info / About, Idle Timeout (view only for Operator). Items visible to **Admin only**: License, Categories, Field Configuration, Admin Password, Operator PIN, Backup & Restore. |
| FR-71 | P2 | The Settings screen shall display the app version number and the current license status (active / expiring soon / expired) under App Info, sourced from the license token (NFR-32, NFR-35). |

#### 4.5.2 Category Management

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-72 | P1 | Admin shall be able to access a **Category Management** screen from Settings, displaying the current list of configured Category values (Section 1.7). |
| FR-73 | P1 | Admin shall be able to **add** a new Category by entering a name, **edit** an existing Category's name, and **delete** a Category, each with appropriate validation (e.g. non-empty, non-duplicate name) and a confirmation prompt for delete. |
| FR-74 | P1 | If a Category is deleted while linked items or product master records reference it, the system shall warn the Admin of the number of affected records and require explicit confirmation before proceeding. Affected records shall retain the deleted Category value as free text (not silently reassigned), unless the Admin selects a replacement Category as part of the deletion flow. |
| FR-75 | P2 | Admin shall be able to **bulk-upload** Category values via a CSV file (single column: Category Name), using the same file-picker and validation pattern as Bulk Linking (Section 4.1.2). Duplicate values (case-insensitive) against the existing list shall be skipped, with a summary shown after import. |
| FR-76 | P3 | Operator role shall have **read-only** access to view the Category list (per RBAC, Section 5.5) but the Add/Edit/Delete/Bulk Upload controls shall not be shown. |

#### 4.5.3 First-Run Setup Flow

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-77 | P1 | On first launch after installation, the app shall present a sequential, non-skippable setup flow consisting of four steps: **(1) License Activation** → **(2) Admin Account Setup** → **(3) Domain Field Configuration** → **(4) Initial Category Setup**. No other screen shall be accessible until the flow is complete. The current step number and total (e.g. "Step 2 of 4") shall be shown on each screen. A "Back" control shall allow returning to a previous step without losing entered data, except Step 1 (license) cannot be revisited once successfully activated. |

#### Step 1 — License Activation

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-77a | P1 | The License Activation screen shall display the device's hardware ID (Android ID / serial) prominently, with a "Copy" action, so the user can share it with the vendor to obtain a license token (per NFR-31). |
| FR-77b | P1 | The screen shall provide two input methods: (a) a multi-line text field to paste a Base64 license token, or (b) an "Import .lic file" button using the Android file picker. |
| FR-77c | P1 | On submission, the app shall verify the token per NFR-33–NFR-35 (signature, device binding, expiry). On success, proceed to Step 2. On failure, display a specific error: invalid signature, device mismatch (showing both expected and actual device IDs), or expired token — with no partial progress. |
| FR-77d | P2 | A "Activate later / Continue offline" option shall **not** be offered — license activation is mandatory before proceeding (per NFR-31, "all app functionality shall be locked"). |

#### Step 2 — Admin Account Setup

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-77e | P1 | The Admin Setup screen shall collect: **Admin username**, **Admin password**, **confirm password**, and selection + answers for **two security questions** from the proposed set (OQ-28). |
| FR-77f | P1 | Password shall meet a minimum complexity rule (configurable; default: minimum 8 characters, at least one letter and one number). The confirm-password field shall be validated to match before allowing "Next". |
| FR-77g | P1 | The two selected security questions must be distinct from each other. Answers shall be stored as salted hashes (NFR-20j), not plain text. |
| FR-77h | P2 | The **shared Operator PIN** shall also be set on this screen (numeric, 4–8 digits per OQ-29), as Operator access is enabled by default. The Admin may instead choose to disable Operator access at this step (toggle), configurable later via Settings (NFR-20i). |

#### Step 3 — Domain Field Configuration [NEW]

This step implements the configurable-schema requirements of Section 1.7 / NFR-51 at install time, so the Linking, Assets, and Inventory screens render correctly from first use.

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-77i | P1 | The screen shall display the **always-on fixed fields** (Name, Barcode, Category, RFID Tag ID) as a non-editable reference list, confirming these are always present. |
| FR-77j | P1 | The Admin shall be able to define zero or more **domain-specific fields**, each with: field key (auto-derived from label, editable), display label, data type (Text / Number / Dropdown / Date), mandatory flag, and visibility flags — which of Linking form, Assets list/detail, Inventory summary, and CSV import/export the field appears in. |
| FR-77k | P2 | For fields of type **Dropdown**, the screen shall allow entering the list of allowed values (comma-separated or one-per-line), editable later via the same mechanism used for Category values (FR-73 pattern). |
| FR-77l | P3 | The screen shall offer a small set of **starter templates** (e.g. "Jewellery: Net Weight, Gross Weight, Purity", "Toy Retail: Price", "Generic: none") that pre-fill the field list, which the Admin can then edit, add to, or clear. Selecting a template does not restrict future changes. |
| FR-77m | P1 | The Admin may proceed with **zero domain-specific fields** defined (fixed fields only); this is valid and fields can be added later via a Settings → Field Configuration screen (new Admin-only Settings item, mirroring S17/Category Management). |
| FR-77n | P1 | Field definitions entered here shall be persisted as the metadata configuration referenced by NFR-51–NFR-54 before proceeding to Step 4. |
| FR-77o | P1 | **Article ID usage toggle:** Separately from the domain-specific field list, this screen shall present a single control for **Article ID usage** with three options — **Not Used / Optional / Mandatory** (Section 1.7, NFR-56). The default selection shall be **Optional**. This setting shall be persisted alongside the field configuration metadata before proceeding to Step 4, and shall determine Article ID's presence and validation behaviour across the Linking form, Bulk Linking CSV, Product Master, Assets, Inventory results, and report CSV export (FR-09c, FR-16, FR-22, FR-23, FR-44, FR-45, FR-50, FR-62–FR-68). |

#### Step 4 — Initial Category Setup

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-78 | P1 | This step shall present a proposed seed Category list (per OQ-03), selectable based on the templates chosen in Step 3 (if any) or a generic default list otherwise. The Admin may edit/remove/add entries inline, or skip with an empty list and configure categories later via Settings → Categories (FR-72–FR-75). |
| FR-78a | P2 | The screen shall also offer the **Bulk Upload (CSV)** option (same pattern as FR-75) as an alternative to inline editing, for clients with a large pre-defined category list. |

#### Completion

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-79 | P2 | On completion of all four steps, the app shall display a brief summary (license status, Admin username set, number of domain fields configured, number of categories configured) with a "Finish" action, then navigate to the PIN/password login screen (Section 5.5). |
| FR-80 | P2 | **License renewal**: after first-run setup, the Admin may update or re-activate the license token at any time via Settings → License, using the same entry mechanism as Step 1 (FR-77b). This is the sole re-entry point for license management after initial setup — no separate Superadmin role or hidden access mechanism is required. |
| FR-80a | P3 | **Domain field reconfiguration**: after first-run setup, the Admin may add, edit, or remove domain-specific fields at any time via a new Settings → Field Configuration screen, using the same interface as Step 3 (FR-77i–FR-77n). Changes apply immediately per NFR-52–NFR-53 (existing records get `null` for new fields). *(v3.15: the Article ID usage toggle referenced here is dropped — see Section 1.7.)* |

---

### 4.6 Functional Flow and Navigation

#### 4.6.1 Purpose

This section defines the complete screen inventory, navigation structure, and screen-level behaviour for StockBuddy. It is derived from a reference application (built for a jewellery retail client) whose screenshots were reviewed during requirements gathering, adapted to this domain-agnostic model (Section 1.7) and to the scope defined in Sections 1–4 of this SRS. Several screens did not exist in the reference application and are specified here as new requirements (marked **[NEW]**).

#### 4.6.2 Global Elements

- **Status bar**: All screens display a persistent "Reader Connected" status indicator confirming the C72's UHF RFID reader hardware connection (formalised below as FR-81).
- **Branding**: Launch screen displays the Gigakin brand (www.gigakin.com) in place of any reference-app branding.
- **Back navigation**: All sub-screens provide a back arrow (top-left) returning to the previous screen in the hierarchy. Home is the hub; all 5 core feature flows plus Settings branch from and return to Home.

| Req ID | Priority | Requirement |
|--------|----------|-------------|
| FR-81 | P1 | **Hardware connection status indicator:** The persistent status bar element shall reflect three states: **Connected** (RFID reader hardware detected and responding), **Not Connected** (reader hardware present but not responding/initialized), and **Not Available** (no RFID reader hardware detected at all — e.g. a non-C72 device or an Android emulator). The indicator shall update in real time as availability changes (e.g. app backgrounded/foregrounded, reader power-cycled). |
| FR-81a | P1 | **Graceful degradation when hardware is unavailable:** When the reader is in the Not Connected or Not Available state, all RFID-dependent actions (the UHF "Scan" button in Individual Linking, the Inventory START button) shall be disabled with a clear inline message (e.g. *"RFID reader not detected — connect the C72 reader to continue"*) rather than allowed to proceed and fail silently or crash. Barcode/QR imager-dependent actions shall follow the same pattern if the imager is unavailable. This degradation shall not block any non-hardware-dependent feature — Category Management, Field Configuration, Bulk Linking via CSV, browsing past results, or Settings shall remain fully usable regardless of reader state. |

#### 4.6.3 Screen Inventory

| ID | Screen | Entry Point | Type |
|----|--------|-------------|------|
| S01 | Launch / Splash | App icon | Existing (rebranded) |
| S02 | PIN Login | After Launch (or after first-run setup, FR-77) | **[NEW]** |
| S03 | Home | After login | Existing |
| S04 | Linking Options (bottom sheet) | Home → Linking | Existing |
| S05 | Individual Linking — Entry Choice | S04 → Individual Linking | **[NEW]** |
| S06 | Individual Linking — Manual Form | S05 → Manual Entry | Existing (fields adapted per Section 1.7) |
| S07 | Individual Linking — QR Scan | S05 → QR Scan | **[NEW]** |
| S08 | Bulk Linking | S04 → Bulk Linking | Existing |
| S09 | Assets List | Home → Assets | Existing |
| S10 | Asset Edit | S09 (Admin only) | **[NEW]** |
| S11 | Enter Inventory Code (modal) | Home → Inventory | Existing |
| S12 | Inventory Scanning (Start/Stop) | After S11 | Existing |
| S13 | Inventory Summary | After Stop (S12), or Reports list (S15) | Existing |
| S14 | Export Report (modal) | S13 → Export icon | Existing (extended) |
| S15 | Inventory Reports List | Home → Reports | Existing |
| S16 | Settings | Home → Settings | **[NEW]** (FR-69–71) |
| S17 | Category Management | S16 (Admin only) | **[NEW]** (FR-72–76) |
| S18 | Backup & Restore | S16 | **[NEW]** |
| S19 | First-Run Setup Wizard | First launch only | **[NEW]** (FR-77–80) |
| S20 | Field Configuration | S16 (Admin only) | **[NEW]** (FR-80a) |

#### 4.6.4 Navigation Map (Description)

```
First launch only:
S19 First-Run Setup Wizard (License -> Admin Setup -> Initial Categories)
  -> S02 PIN Login

S01 Launch
  -> S02 PIN Login
       -> S03 Home (hub)
            -> S04 Linking Options
                 -> S05 Individual Linking Entry Choice
                      -> S06 Manual Form  <-> S07 QR Scan (fallback on partial QR)
                 -> S08 Bulk Linking
            -> S09 Assets List
                 -> S10 Asset Edit (Admin)
            -> S11 Enter Inventory Code
                 -> S12 Inventory Scanning
                      -> S13 Inventory Summary
                           -> S14 Export Report (modal)
            -> S15 Reports List
                 -> S13 Inventory Summary (historical session)
            -> S16 Settings
                 -> S17 Category Management (Admin)
                 -> S18 Backup & Restore
                 -> S20 Field Configuration (Admin)
                 (License renewal, Admin Password, Operator PIN -- inline within S16, Admin only)
```

Note: S13 (Inventory Summary) has two incoming paths — directly after stopping a live scan, or by opening a past report from S15 — and renders identically in both cases. S14 (Export) is a modal overlay on S13, accessible from either path. S19 is shown only once, on first launch after installation; subsequent launches go directly to S02.

#### 4.6.5 Screen-by-Screen Detail

**S01 — Launch / Splash**
Displays Gigakin branding while the app initializes and confirms RFID reader connection. Transitions automatically to S02 (or S19 on first launch).

**S02 — PIN Login [NEW]**
Single credential entry field per NFR-20b (Operator PIN or Admin password, role determined by match). On successful entry, the user's role (Admin / Operator) is determined and used to gate visibility of Settings sub-screens (S17–S18, plus License/Password/PIN items in S16) and Admin-only actions (asset edit/delete in S09/S10). Failed attempts show an error and allow retry, subject to lockout (NFR-20c).

**S03 — Home**
Grid menu with tiles: Linking, Assets, Reports, Inventory, and Settings **[NEW tile]**. "Product Info" tile from the reference app is excluded per current scope (Section 1.2).

**S04 — Linking Options**
Bottom sheet presenting two choices: Individual Linking, Bulk Linking.

**S05 — Individual Linking: Entry Choice [NEW]**
On selecting "Individual Linking," the user is presented with two entry modes: **Manual Entry** (→ S06) or **Scan QR Code** (→ S07). This screen did not exist in the reference app; QR scanning was the original sole input mechanism per earlier SRS versions, and manual entry was added in v3.4 as an additional mode.

**S06 — Individual Linking: Manual Form**
Form fields per Section 1.7: fixed fields (Name, Barcode, Category), Article ID (where enabled for the deployment, per its configured mode — NFR-56) plus any configured domain-specific fields (e.g. Price), plus RFID. Barcode supports manual entry or imager scan; RFID supports manual entry or UHF "Scan" button. **Save Link** validates and saves the record.
- On success: confirmation message shown; all fields reset; user remains on S06 to link the next item.
- On error (e.g. duplicate RFID, missing required field, or missing Article ID when its mode is Mandatory): error message shown inline; fields retain entered values for correction.

**S07 — Individual Linking: QR Scan [NEW]**
Activates device camera to scan a QR code printed on the item label. On successful scan:
- If the QR payload contains all required fields (per OQ-01 schema, to be confirmed), the record is saved directly (same success/error handling as S06) and the screen remains active for the next scan.
- If the QR payload is **partial** (some fields missing or unreadable), the app switches to S06 (Manual Form) with the successfully parsed fields pre-populated, allowing the user to complete the remaining fields manually.
- A control to switch to Manual Entry (S06) is available at any time.

**S08 — Bulk Linking**
Displays required CSV column format per Section 1.7 (fixed fields + Article ID column if enabled + configured domain-specific fields + RFID). User selects a CSV file via file picker; selected filename is displayed. "Import Data" is disabled until a file is selected.
- On Import: validates each row (required columns present, data types, duplicate RFID against existing master data and within the file itself; duplicate Article ID is not treated as an error since Article ID is not a uniqueness key).
- On completion: shows import summary (rows imported successfully / rows with errors), with the ability to view/download an error detail report for failed rows.

**S09 — Assets List**
Displays all linked items as cards (Name, Barcode, RFID, Article ID where enabled, plus configured domain-specific fields per card). Search bar filters across **Name, Category, Barcode, RFID Tag ID, and Article ID (where enabled)**.
- **Admin role**: each item/card supports Edit (→ S10) and Delete (with confirmation). Deleting a linked item removes the RFID-to-product association only; the underlying product master record is unaffected (per v3.4 scope note).
- **Operator role**: read-only list and search; no edit/delete controls shown.

**S10 — Asset Edit [NEW]**
Admin-only. Form pre-populated with the selected item's current fixed fields, RFID, and configured domain-specific fields, editable and savable with the same validation rules as S06.

**S11 — Enter Inventory Code (modal)**
Triggered from Home → Inventory. Modal text input for the session name/code. Cancel returns to Home; OK starts a new session and navigates to S12 with counter reset to 0.

**S12 — Inventory Scanning**
Displays the session name, a category filter dropdown ("All" / specific category — a post-scan display filter per v2.6, not a scan-time restriction), and a live count of unique RFID tags scanned.
- **START** (green) begins scanning via the device's UHF trigger; the count updates live.
- **STOP** (red) is shown while scanning is active. Tapping STOP opens a confirmation dialog ("Stop scanning and generate summary?"). Confirming ends the session and navigates to S13; cancelling resumes scanning.

**S13 — Inventory Summary**
Header displays "Inventory " + the session code entered in S11 (e.g. "Inventory 3 June"). Reachable either immediately after S12 (live session) or by selecting a session from S15 (historical session) — identical layout in both cases.
- **Summary card**: Total in Master, Total Scanned, and three counts — Available, Missing, Excess.
- **Tabs**: Available, Missing, and **Excess [NEW]**.
  - Available / Missing tabs list items grouped per FR-45, showing fixed fields plus configured domain-specific fields.
  - Excess tab **[NEW]**: lists scanned RFID Tag IDs with no matching master record — displayed as a flat list of RFID values (no product detail available), since these tags are unrecognised.
- **Export icon** (top-right) opens S14.

**S14 — Export Report (modal)**
Confirmation dialog: "Do you want to export Inventory <code> as CSV?" with Cancel / Export.
- On Export, file is generated as `Inventory_Report_<inventory code>.csv`, including Available, Missing, and Excess sections (per OQ-04, pending confirmation on exact CSV structure).
- Per Section 1.2 (Feature 5) and FR-49, this modal must offer four delivery channels: **Download** (save to device with a location picker, per FR-52a, as in the reference app), **Share** (Android share sheet, FR-52), **API upload** (FR-53–54), and **LAN transfer** (FR-55–56).

**S15 — Inventory Reports List**
Lists past sessions as cards: session code/name and timestamp. Tapping a card opens that session in S13.

**S16 — Settings [NEW]** (FR-69–71)
New Home tile. Menu list, with items role-gated per FR-70:
- License — status display (all roles); renewal/re-activation (Admin only, FR-80)
- Categories → S17 (Admin)
- Field Configuration → S20 (Admin)
- Admin Password — change (Admin only, NFR-20f)
- Operator PIN — set/view/change, enable/disable Operator access (Admin only, NFR-20h, NFR-20i)
- Idle Timeout — configurable (Admin), view-only (Operator)
- Backup & Restore → S18
- App Info / About (all roles, FR-71)

**S17 — Category Management [NEW]** (FR-72–76)
Admin-only. List of configured categories (seed list per OQ-03) with add/edit/delete (one at a time, FR-73), deletion-impact warning (FR-74), plus a **Bulk Upload (CSV)** option (FR-75) reusing the S08 pattern. Operator has read-only view (FR-76).

**S18 — Backup & Restore [NEW]**
Displays backup status (last backup time, tier/destination per Section 5.9), manual "Backup Now" trigger, and "Restore from Backup" with file/source selection and confirmation.

**S19 — First-Run Setup Wizard [NEW]** (FR-77–80)
Shown automatically on first launch after installation, before S02. Four sequential, non-skippable steps (each showing "Step X of 4"):
1. **License activation** — enter/import token, device ID display (FR-77a–FR-77d)
2. **Admin account setup** — username, password, security questions, Operator PIN (FR-77e–FR-77h)
3. **Domain field configuration** — define domain-specific fields and starter templates (FR-77i–FR-77n)
4. **Initial Category setup** — review/edit seed list or bulk upload (FR-78, FR-78a)

Completion screen summarises the configuration, then proceeds to S02 (PIN Login). Not re-entrant after completion; subsequent changes are made via S16 → License (FR-80) or S16 → Field Configuration (S20, FR-80a).

**S20 — Field Configuration [NEW]** (FR-80a)
Admin-only. Reuses the Step 3 interface (S19) to add, edit, or remove domain-specific field definitions after initial setup. Lists current fields with their data type, mandatory flag, and visibility settings; supports the same starter-template option as S19 Step 3.

#### 4.6.6 Open Items Carried Forward

The following items, raised during screen review, require resolution and are also reflected in Section 7 (Open Items and Questions):
- ~~Exact QR payload schema for S07 (OQ-01)~~ — **RESOLVED (v3.17): JSON schema, see OQ-01 / `util/QrPayload.kt`**
- Excess item CSV structure — separate section vs. interleaved (OQ-04)
- Export channel selection UX in S14 — single modal with channel choice, or sequential screens (relates to OQ-10–OQ-12)

---

## 5. Non-Functional Requirements

### 5.1 Performance

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-01 | < 1 second | Each unique RFID tag detected by the C72 antenna shall appear in the real-time count within 1 second of detection. |
| NFR-02 | < 3 seconds | The full results summary (Available / Missing / Excess counts and item lists) shall be computed and displayed within 3 seconds of the user pressing STOP, for a master size of up to 10,000 items. |
| NFR-03 | < 3 seconds | Bulk CSV import of up to 5,000 rows shall complete within 3 seconds. |
| NFR-04 | < 2 seconds | CSV export file generation for up to 10,000 rows shall complete within 2 seconds. |
| NFR-05 | < 2 seconds | App cold start to the home screen on the C72 shall take less than 2 seconds. |
| NFR-06 | 500+ tags/min | The RFID scan engine shall process at least 500 unique tag reads per minute without dropping tags or freezing the UI. |

### 5.2 Reliability

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-07 | Crash recovery | If the app crashes or is accidentally closed during an active scan session, all tags collected up to that point shall be recoverable on next app launch, with an option to resume or discard the session. |
| NFR-08 | Data persistence | All locally stored item records and session data shall survive device restarts without corruption. |
| NFR-09 | Network-independent core | All core workflows (linking, scanning, summary, local CSV export) shall function fully without any network connection. Network connectivity is required only for API upload and LAN transfer delivery channels; the app shall gracefully notify the user if connectivity is unavailable when those channels are attempted. |

### 5.3 Usability

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-10 | Touch targets | All tappable controls shall have a minimum touch target of 48 × 48 dp. |
| NFR-10a | Design system | All screens shall be built using Material Design 3 (MD3) components and theming — not a custom or ad-hoc visual system. This supersedes the brief MD3 mention previously listed only as a Design Constraint (Section 6). |
| NFR-10b | Device form factor | All layouts shall be designed and verified against a 6-inch-class display in portrait orientation — the C72's screen size — rather than assuming a typical phone or tablet aspect ratio. |
| NFR-10c | Primary action sizing | The single highest-frequency action on each screen (UHF Scan, START, STOP, Save Link) shall use a touch target larger than the NFR-10 baseline — minimum 56dp height, full-width or near-full-width where the layout allows. These are the most time-pressured, highest-repetition actions in the app and shall be visually and physically the easiest target to hit. |
| NFR-10d | One-handed thumb-zone placement | On scanning and linking screens, the primary action (UHF Scan / START / STOP) shall be positioned in the lower half of the screen — not in the top app bar or header — so it's reachable by the thumb when the device is held one-handed in a standard grip, consistent with the one-handed-while-moving-through-shelves usage pattern already assumed in Section 1.6. |
| NFR-10e | Screen density | Each screen shall surface no more than one primary action plus the minimum supporting information needed for that action. Secondary actions, detail fields, and rarely-used controls shall be deferred to a secondary screen, expandable section, or modal rather than co-located with the primary action. Field-level scan affordances (Barcode, RFID Tag ID — see NFR-10f) are a defined exception to this rule. |
| NFR-10f | Field-level scan affordances vs. the primary action | The Barcode (FR-06) and RFID Tag ID (FR-07) scan controls on the Individual Linking form are field-level utility actions, not the screen's primary action, and are exempt from the single-primary-action rule (NFR-10e). **Barcode scan** shall be a compact inline icon button adjacent to the field, sized to the NFR-10 baseline touch target. **RFID Tag ID scan** shall be a more visually prominent, clearly labelled "Scan" button rather than a small icon — RFID Tag ID is the field around which the app's entire uniqueness and matching logic is built (FR-08), and its scan action carries richer feedback states (FR-07a: success / no tag detected / multiple tags detected) that warrant a deliberate, unambiguous tap target rather than an icon afterthought. The form's true primary action — **Save Link** — remains the single oversized, thumb-zone-positioned control specified in NFR-10c/10d, distinct from both field-level scan buttons. |
| NFR-11 | Colour coding | Available (green), Missing (red), and Excess (yellow) colour coding shall be applied consistently to KPI chips, list tabs, and item cards throughout the app. |
| NFR-12 | Navigation depth | Starting a new inventory session, accessing Linking, and accessing Inventory Reports shall each be reachable within 3 taps from the home screen. |
| NFR-13 | Error messages | All error and validation messages shall describe the problem in plain language (no raw exception text or stack traces visible to users). |
| NFR-13a | Feedback visibility | Success, error, and confirmation messages (e.g. item saved, RFID scan result, import summary, export result, demo-limit reached) shall be presented with sufficient visual prominence — size, contrast, and positioning — to be clearly noticed under bright retail-showroom lighting, not as subtle or low-contrast UI elements. Messages shall not rely on colour alone to convey meaning (NFR-11's colour coding shall be paired with an icon and/or text label). Transient confirmations (e.g. toasts/snackbars) shall remain visible long enough to be read before auto-dismissing; messages blocking a critical action (e.g. a save-blocking validation error) shall persist until acknowledged or resolved, not auto-dismiss. Messages shall not be positioned where they can be obscured by the on-screen keyboard or by the user's hand/thumb during one-handed operation (consistent with the thumb-zone placement in NFR-10d). |
| NFR-14 | Training time | A store staff member with basic Android familiarity shall be able to independently complete a full link → scan → export cycle after no more than 20 minutes of training. |

### 5.4 Storage and Data Management

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-15 | Local only | All inventory data shall be stored on the device using SQLite (via Room) or an equivalent embedded database. No inventory data shall be transmitted to any remote system automatically. The only permitted automatic network call is the periodic revocation list fetch (NFR-37) — a single HTTP GET to a static JSON file. All other network calls are user-initiated (report delivery via API upload or LAN transfer). |
| NFR-16 | Storage budget | The application including all local data shall not exceed 500 MB of device storage under normal operating conditions. This ceiling shall be reviewed once OQ-17 and OQ-18 are answered and actual volume estimates are confirmed. |
| NFR-17 | CSV retention | Exported CSV files shall be retained in a dedicated local folder for at least 30 days, after which they are eligible for auto-purge. |
| NFR-18 | RFID uniqueness | RFID Tag ID uniqueness shall be enforced at the database layer. Any insert or import that would create a duplicate RFID must be rejected with a clear error. |
| NFR-19a | Session retention policy | The app shall enforce a configurable session data retention window (default: 90 days). Sessions older than the retention window shall be automatically purged from the device, provided their CSV has been successfully exported at least once. Sessions whose CSV has never been exported shall not be auto-purged regardless of age — the app shall instead warn the user. |
| NFR-19b | Storage warning | When device storage used by the app exceeds 80% of the configured storage budget, the app shall display a persistent warning on the home screen and Settings, advising the user to export and purge old sessions or perform a backup. |

### 5.5 Authentication, User Management, and RBAC

The app uses a **PIN-first authentication** model with two roles: Admin and Operator. All credentials are stored locally on the device as salted hashes. There is no remote identity provider.

**Authentication flow:**
- The lock screen presents a single PIN/password entry field with no role selector.
- The app checks the entered value against the Operator PIN first, then the Admin password.
- If it matches the Operator PIN → logged in as Operator.
- If it matches the Admin password → logged in as Admin.
- If neither matches → login rejected; failed attempt counter incremented.

**Role summary:**

| Aspect | Admin | Operator |
|--------|-------|----------|
| Account type | Individual named account | Shared PIN (all floor staff) |
| Credential | Username + alphanumeric password | Numeric PIN |
| Count per device | Exactly one | One shared PIN, used by any number of staff |
| Created by | First-run setup (user-defined) | Admin only |

#### Authentication Requirements

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-20a | First-run setup | On first launch after license activation, the app shall display an **Admin Setup screen** prompting the user to create the Admin account: enter a username, a password, confirm the password, and select and answer two security questions. No other screen shall be accessible until Admin setup is complete. |
| NFR-20b | PIN-first login | The lock screen shall display a single credential entry field. The app shall silently check the entered value against the Operator PIN first, then the Admin password. The role is determined by whichever matches — no explicit role selection is required from the user. |
| NFR-20c | Failed attempt lockout | After 5 consecutive failed login attempts, the app shall lock the login screen for a configurable cooldown period (default: 5 minutes). The remaining cooldown time shall be displayed. This applies to both Admin and Operator login attempts. |
| NFR-20d | Auto-lock | The app shall automatically lock and return to the login screen after a configurable idle period (default: 5 minutes of no user interaction). The idle timeout shall be configurable by the Admin from the Settings screen. |
| NFR-20e | Session continuity | Auto-lock shall not discard any active scan session. The user shall be able to re-authenticate and resume the session — including any RFID tags already collected — without data loss. |
| NFR-20f | Admin password change | The Admin shall be able to change their own password from the Settings screen, requiring entry of the current password before the new password is accepted. |
| NFR-20g | Admin account recovery | If the Admin forgets their password, the app shall provide a recovery flow via the two security questions set during first-run setup. On correct answers, the Admin shall be prompted to set a new password. Incorrect answers shall not reveal any hint and shall increment the failed attempt counter. |
| NFR-20h | Operator PIN management | The Admin shall be able to set, view, and change the shared Operator PIN from the Settings screen. The Operator PIN shall be numeric only (minimum 4 digits). |
| NFR-20i | Operator access control | The Admin shall be able to **disable** Operator access entirely (e.g. when all floor staff are absent or during a sensitive audit). When Operator access is disabled, the shared PIN shall be rejected at login and only the Admin password shall be accepted. The Admin shall be able to re-enable Operator access at any time. |
| NFR-20j | Credential storage | All credentials (Admin password, Operator PIN) shall be stored as salted hashes using a strong one-way hashing algorithm (e.g. bcrypt or PBKDF2). Plain-text credentials shall never be stored or logged. |

#### Role-Based Access Control (RBAC)

| Feature / Action | Operator | Admin |
|-----------------|----------|-------|
| **Linking** | | |
| Individual Linking — scan QR and link item | ✅ | ✅ |
| View linked items list | ✅ | ✅ |
| Edit linked item record | ❌ | ✅ |
| Delete linked item record | ❌ | ✅ |
| Bulk Linking — CSV import | ❌ | ✅ |
| **Product Master** | | |
| View product master | ✅ | ✅ |
| Edit product record | ❌ | ✅ |
| Delete product record | ❌ | ✅ |
| Bulk upload product master CSV | ❌ | ✅ |
| Export product master CSV | ❌ | ✅ |
| **Category Management** | | |
| View category list | ✅ | ✅ |
| Add / remove individual category | ❌ | ✅ |
| Bulk upload category CSV | ❌ | ✅ |
| **Assets** | | |
| View Assets list | ✅ | ✅ |
| Search Assets | ✅ | ✅ |
| Delete linked item from Assets | ❌ | ✅ |
| **Inventory Sessions** | | |
| Create and run inventory session | ✅ | ✅ |
| View session results | ✅ | ✅ |
| Export / send session report (all channels) | ✅ | ✅ |
| Delete past session | ❌ | ✅ |
| **Settings and Configuration** | | |
| View app settings | ❌ | ✅ |
| Configure API / LAN delivery endpoints | ❌ | ✅ |
| Configure idle timeout and session retention | ❌ | ✅ |
| Set / change Operator PIN | ❌ | ✅ |
| Enable / disable Operator access | ❌ | ✅ |
| Change Admin password | ❌ | ✅ |
| **Backup and Restore** | | |
| Backup Now (manual export) | ❌ | ✅ |
| Restore from backup | ❌ | ✅ |
| View backup history | ❌ | ✅ |
| **Licensing** | | |
| Activate / import license token | ❌ | ✅ |
| Renew license (import new token) | ❌ | ✅ |
| View license details (expiry, client name) | ✅ | ✅ |

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-20k | RBAC enforcement | All Admin-only actions listed in the RBAC table shall be inaccessible to an Operator-authenticated session. Admin-only screens and controls shall either be hidden entirely or shown as disabled with the label *"Admin access required"* — not just blocked on tap. |
| NFR-20l | Role visibility | The current logged-in role (Admin or Operator) shall be persistently displayed on the home screen (e.g. in the app header or status bar) so the user is always aware of their access level. |
| NFR-20m | Audit trail | All actions that modify data — linking, editing, deleting, importing, exporting, backup, restore, settings changes — shall be logged in a local audit trail with: timestamp, role (Admin/Operator), and action description. The audit trail shall be viewable and exportable by the Admin only. |

#### Data at Rest

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-20n | Data at rest | Locally stored master data, session records, and credentials shall be protected within Android's app sandbox. No sensitive data shall be written to publicly accessible external storage paths. |
| NFR-20o | Audit log retention | Audit log entries shall be retained for a rolling **90-day window**, consistent with the session data retention policy (NFR-19a). A background job shall automatically delete entries older than 90 days. The retention window shall be configurable by the Admin (minimum 30 days, maximum 365 days). Admin shall be able to export the full audit log before purge via Settings → App Info → Export Audit Log. |

### 5.6 Hardware Compatibility

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-21 | Primary device | The certified deployment target is the Chainway C72 Android UHF RFID handheld. |
| NFR-22 | Android version | The app shall support Android 9 (API 28) and above. |
| NFR-23 | RFID SDK | The app shall integrate with the C72 UHF RFID module via Chainway's official Android SDK. |
| NFR-24 | Imager | The app shall use the C72 imager for QR code and barcode scanning during Individual Linking. |
| NFR-25 | Battery | Active RFID scanning shall not drain the device battery faster than 15% per hour. |
| NFR-26 | AOSP | The app shall not require Google Play Services. All features shall work on AOSP builds as shipped on the C72. |
| NFR-26a | Non-hardware testability | The app shall run without crashing on an Android emulator or any device lacking the Chainway RFID SDK / UHF hardware, with RFID- and imager-dependent features gracefully disabled per FR-81a. This enables full UI/UX and non-hardware feature testing (Field Configuration, Category Management, Bulk Linking, results browsing, export, Settings) without requiring a physical C72 for every test cycle. Verification against the actual UHF scan and imager hardware shall still be performed on a physical C72 before any release. |

### 5.7 Maintainability

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-27 | Architecture | The app shall follow a layered architecture (UI → ViewModel → Repository → Room DB) to allow independent module testing and updates. |
| NFR-28 | Language | The application shall be written primarily in **Kotlin** — the standard modern language for Android development (concise syntax, null-safety, first-class Jetpack/Architecture Components support). **Java** is used directly wherever needed for third-party SDK integration, most notably the Chainway UHF SDK, which ships as Java APIs/AARs. Kotlin's full bidirectional interoperability with Java means the SDK can be called directly from Kotlin code with no wrapper layer required; Java may also be used for any other third-party library with no idiomatic Kotlin equivalent. This reverses the Java-only constraint set in v3.1, reflecting an updated language strategy decision. |
| NFR-29 | Logging | The app shall maintain a rolling local debug log (last 7 days) that can be exported and shared by the user for remote diagnostics. |
| NFR-30 | Updates | The app shall support APK-based sideload updates without requiring a data wipe. |

### 5.8 Licensing

The app uses a **hybrid offline/lightweight-online licensing model**. Core validation (signature, device binding, expiry) is performed entirely on-device with no server required. The only online component is a periodic fetch of a static **revocation list** — a JSON file hosted at a vendor-controlled URL — which enables the vendor to cancel a specific license token when needed (e.g. on reported device loss).

---

**How a license is issued:**
1. The vendor generates a license payload containing a unique Token ID and signs it with a private key, producing a Base64-encoded license token.
2. The token is delivered to the client via any channel (email, WhatsApp, etc.).
3. The client pastes the token into the Activation screen or imports a `.lic` file.
4. The app verifies the signature offline, checks device ID binding, and checks expiry — all on-device.

**How revocation works:**
1. The vendor maintains a simple JSON revocation list at a fixed URL (e.g. a GitHub Gist, S3 bucket, or any static file host):
   ```json
   { "revokedTokens": ["token-uuid-abc123", "token-uuid-xyz789"], "updatedAt": "2026-06-12" }
   ```
2. The app fetches and caches this list periodically (every 7 days).
3. On each launch, the app checks whether the active token's ID appears in the cached revocation list.
4. If the token is revoked, the app locks immediately with the message: *"Your license has been cancelled. Please contact your vendor."*

**Device loss / replacement scenario:**

> *A client reports their C72 device is lost or damaged and requests a new license for a replacement device.*

The vendor's response:
1. Add the old token's ID to the revocation list → the old device (if it still exists and connects to the internet) will lock within 7 days.
2. Issue a new signed token for the new device ID.
3. Client activates the new device with the new token.

If the client is bluffing and still has the old device:
- The old token is now revoked — it will lock on the old device within 7 days (next scheduled revocation check).
- The grace period (NFR-37) ensures honest clients with no connectivity are not punished, but it also provides a bounded window after which the old device locks regardless.
- Additionally, since revocation is checked at every CSV export attempt (NFR-37b), a client who needs the device to do its primary job — exporting inventory reports — cannot avoid the revocation check indefinitely.

The client therefore cannot maintain two simultaneously working installations from a single license.

---

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-31 | Activation required | On first launch after installation, the app shall display an **Activation screen** prompting the user to enter or import their license token. All app functionality shall be locked until a valid license is activated. The activation screen shall display the device's hardware ID (Android ID or device serial) so the user can share it with the vendor to obtain a license. |
| NFR-32 | License token format | The license token shall be a Base64-encoded, vendor-signed payload containing: **Token ID** (UUID, unique per issued license), **client name**, **device ID**, **issue date**, and **expiry date**. The token shall be importable either by (a) pasting a Base64 string directly into the activation screen, or (b) importing a `.lic` file via the Android file picker. |
| NFR-33 | Cryptographic verification | The app shall verify the license token's digital signature using an RSA or ECDSA public key embedded in the APK at build time. A token with an invalid or missing signature shall be rejected immediately — no partial activation. |
| NFR-34 | Device binding | The app shall verify that the `deviceId` field in the license token matches the device's own hardware ID. A token issued for a different device shall be rejected with the message: *"This license is not valid for this device. Please contact your vendor."* |
| NFR-35 | Expiry | The app shall check the license expiry date on every launch against the device clock. Warnings shall be shown in the app header and Settings screen 14 days and 7 days before expiry: *"Your license expires on [date]. Please contact your vendor to renew."* Once expired, all data-entry and scanning functions shall be locked. Read-only access to past session reports and linked item records shall be retained after expiry to prevent data loss. |
| NFR-36 | Renewal | License renewal is performed by the vendor issuing a new signed token with an updated expiry date. The client imports the new token via the activation/import screen, replacing the existing license without requiring a reinstall or data wipe. |
| NFR-37 | Revocation list fetch | The app shall fetch the vendor's revocation list (a static JSON file at a configured URL) once every 7 days and cache it locally. On every launch, the app shall check whether the active token's Token ID appears in the cached revocation list. If the token ID is found, the app shall immediately lock all functionality with the message: *"Your license has been cancelled. Please contact your vendor."* Read-only access to past data shall be retained. |
| NFR-37a | Revocation grace period | If the revocation list cannot be fetched (no network, server unreachable), the app shall continue to function normally for a grace period of **14 days** from the last successful fetch. After the grace period expires without a successful fetch, the app shall lock and display: *"License verification overdue. Please connect to the internet to verify your license."* This prevents a client from permanently avoiding revocation by blocking network access, while protecting honest clients with intermittent connectivity. |
| NFR-37b | Revocation check on export | Regardless of the 7-day schedule, the app shall perform a fresh revocation list fetch every time the user attempts a network-dependent action (API upload or LAN transfer). If the fetch returns a revoked status for the active token, the export shall be blocked and the app locked immediately. This ensures a client cannot avoid revocation by keeping the device offline while still using it for its primary workflow. |
| NFR-37c | Device loss / replacement | When a client reports device loss or damage and requests a new license: (a) the vendor adds the old token's ID to the revocation list — the old device locks within 14 days (the grace period); (b) the vendor issues a new signed token for the new device ID; (c) the client activates the new device. If the client misrepresents the loss, the old device will lock on its next revocation check or at the grace period boundary — whichever comes first — ensuring both old and new tokens cannot remain active simultaneously. |
| NFR-38 | Transfer restriction | A license token is bound to a specific device ID. Installing the APK on a different device shows the new device's hardware ID on the activation screen, requiring the client to obtain a new token from the vendor. No user-side transfer or re-bind mechanism shall exist. |
| NFR-39 | Tamper resistance | The embedded public key and locally stored license token shall be protected: the APK shall be built with ProGuard/R8 obfuscation, and the license token shall be stored in the app's private internal storage. The cached revocation list shall also be stored in private internal storage and shall not be user-modifiable. |

### 5.9 Backup and Restore

The app implements a three-tier backup strategy to protect against device loss, damage, or failure. All three tiers are independent and can coexist. The appropriate tier(s) for a given deployment depend on store infrastructure (answered via OQ-23 to OQ-27).

**What is backed up:**

| Data | Backup priority | Notes |
|------|----------------|-------|
| SQLite DB (product master + linked items + sessions) | Critical | Hard or impossible to recreate manually |
| Exported CSV files | Medium | May already exist at head office |
| License token (`.lic`) | Low | Vendor can re-issue for a new device |

**Tier 1 — Automatic local backup (always on, no connectivity needed)**

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-39 | Daily local backup | The app shall automatically create a compressed snapshot of the SQLite database once per day (default: at midnight or on first app launch of the day, whichever comes first). The snapshot shall be saved to the device's external SD card if present, or to a dedicated folder in internal storage if no SD card is available. |
| NFR-40 | Local backup retention | The app shall retain the last 7 daily local backup snapshots, automatically deleting older ones to conserve storage. |
| NFR-41 | Backup integrity | Each backup snapshot shall include a checksum. On restore, the app shall verify the checksum before applying the backup and reject corrupted files with a clear error. |

**Tier 2 — Manual backup via share sheet (on-demand, any connectivity)**

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-42 | Manual backup export | The Settings screen shall include a **"Backup Now"** button that generates a single encrypted `.zip` archive containing the full SQLite DB snapshot and all locally retained CSV files. |
| NFR-43 | Share sheet delivery | After generating the backup archive, the app shall invoke the Android share sheet so the user can send it to any destination — Google Drive, email, WhatsApp, USB file manager, or any installed app. |
| NFR-44 | Pre-update prompt | When an APK update is detected or initiated, the app shall prompt the user to perform a manual backup before proceeding with the update. |

**Tier 3 — Automatic remote backup (optional, requires Wi-Fi or LAN)**

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-45 | Automatic remote backup | If a LAN or API destination is configured in Settings (already used for CSV delivery), the app shall optionally send a nightly DB snapshot to the same destination. This feature shall be opt-in and disabled by default. |
| NFR-46 | Remote backup failure handling | If a scheduled remote backup fails (network unavailable, server unreachable), the app shall silently retry up to 3 times over 24 hours. If all retries fail, the app shall display a non-blocking notification on next launch. The failure shall never interrupt the user's core workflow. |

**Restore**

| Req ID | Target | Requirement |
|--------|--------|-------------|
| NFR-47 | Restore from backup | The Settings screen shall include a **"Restore from Backup"** option. The user shall be able to select a `.zip` backup archive from the device file system (downloaded from cloud storage, received via email/WhatsApp, or copied via USB). |
| NFR-48 | Restore validation | Before applying a restore, the app shall display a summary of the backup contents: creation date, number of product records, number of linked items, number of sessions. The user must explicitly confirm before the restore overwrites current data. |
| NFR-49 | Restore scope | A full restore shall replace the entire local DB with the backup contents. The app shall remain functional immediately after restore completes without requiring a reinstall. The license token shall not be overwritten by a restore — it must be re-activated separately if the device has changed. |
| NFR-50 | Restore time | Restore from a backup archive of up to 500 MB shall complete within 30 seconds on the C72 device. |

---

## 6. Design Constraints and Guidelines

- **Application ID:** `com.gigakin.stockbuddy`.
- **From-scratch build:** No existing codebase is being ported. The reference application mentioned in Section 4.6.1 was used only for UI/UX layout reference (screenshots reviewed during requirements gathering) — no source code from it is reused.

- **Local-first, always:** The app must never initiate a network call as part of any core user workflow. Network use is only acceptable when the user explicitly shares/exports a file via the Android share sheet.
- **Session naming is user-driven:** The Inventory Code is a free-text name entered by the user at session creation (e.g. "3 June Morning", "Shelf B Check"). The system shall not auto-generate opaque codes.
- **Results are immutable after STOP:** Once a scan session is stopped and results are computed and saved, the underlying tag list for that session must not be modifiable.
- **Domain-agnostic product schema:** The data model's always-on fixed fields are Name, Barcode, and Category (Section 1.7). Article ID is also a fixed field, but its presence is configured per deployment (Not Used / Optional / Mandatory — NFR-56) rather than always-on, since not every industry pre-prints a unique per-unit identifier. Domain-specific fields (e.g. Price for toy retail; weight/purity for jewellery) are configured per deployment via metadata and must not be hardcoded into the app.
- **Category list must be pre-seeded** with toy-relevant defaults reflecting Crazy Hamster's product range: Ride-On, Board Game, Educational, Magic Car, Hot Wheels, Craft Kit, Soft Toy, Action Figure, Outdoor, Other. The list must be user-extensible.
- **No Google Play Services dependency:** The C72 typically runs AOSP. Libraries requiring GMS (Firebase, Google Maps, etc.) must not be used.
- **Material Design 3, rugged-device ergonomics:** See Section 5.3 (NFR-10, NFR-10a–e) for the full formal specification — MD3 components, 6-inch form factor, oversized primary-action buttons, thumb-zone placement, and screen decluttering.
- **Category filter is post-scan, not pre-scan:** The C72 UHF antenna reads all RFID tags in range indiscriminately — there is no hardware mechanism to restrict scanning to a specific category. Category filtering must always be implemented as a post-scan operation applied to the stored raw tag set. The raw tag set for a session must never be discarded or truncated based on a filter selection; it must be retained in full so the filter can be changed and re-applied without re-scanning.
- **Licensing is hybrid offline/lightweight-online:** Core validation (signature, device binding, expiry) is fully offline. The only network touch is a periodic HTTP GET to fetch a static JSON revocation list — no server logic, no database, no API. The revocation list can be hosted on a GitHub Gist, S3 bucket, or any static file host. The vendor's only operational responsibilities are: (a) safeguarding the private key, (b) running a local license generator tool to sign tokens, and (c) editing the revocation JSON file when a token needs to be cancelled.
- **Locally stored data must survive license expiry or revocation:** Locking the app must never result in data loss. Past session reports and linked item records shall remain readable in read-only mode so the client can export their data before renewing or migrating.
- **PIN-first authentication — no role selector:** The login screen must not ask the user to choose their role before entering credentials. Role is determined implicitly by which credential matches — Operator PIN first, Admin password second. This keeps the login flow fast for floor staff while maintaining a clear role separation.
- **Exactly one Admin per device:** The app must enforce that only one Admin account exists at any time. There is no mechanism to create a second Admin, promote an Operator to Admin, or transfer Admin ownership. If the Admin account needs to be reset, the vendor-assisted recovery flow (security questions) is the only path.
- **Barcode can be duplicate across units:** The same Barcode value (e.g. `B1`) will legitimately appear on multiple items of the same product. RFID Tag ID is the uniqueness key, not Barcode.
- **Backup archives must be encrypted:** The `.zip` backup archive generated by Tier 2 (manual) and Tier 3 (remote) shall be AES-256 encrypted before leaving the app's private storage. The encryption key shall be derived from a combination of the device ID and a vendor-defined salt embedded in the APK — ensuring the backup can only be restored on the originating device or a device with the same vendor key. This prevents raw DB access if the backup file is intercepted.
- **Session auto-purge must be export-gated:** The app must never automatically delete a session whose CSV has not been exported at least once. Purge eligibility is always conditioned on confirmed export, not just age.

---

## 7. Open Items and Questions

| # | Priority | Open Question / Decision Required |
|---|----------|----------------------------------|
| ~~OQ-01~~ | P1 | **RESOLVED (v3.17).** The QR payload is a **JSON object**: `{ "productName", "barcode", "category", "rfid", "attributes": { "<fieldKey>": "<value>" } }` — parsed by `util/QrPayload.kt`. It carries the fixed fields (Product Name, Barcode, Category, RFID Tag ID; **no Article ID** — dropped per v3.15) plus any configured domain-specific fields (Section 1.7) inside `attributes`. Any field may be absent (**FR-04** partial payload → the user completes the pre-filled Individual Linking form). The warehouse/supplier QR-generation tooling must emit this schema. *Original open question:* exact payload format / owner of the QR-generation spec — now fixed to the JSON schema above; the label-printing/generation tool remains the deploying party's responsibility. |
| OQ-02 | P1 | Is "Article ID" a business-assigned identifier printed on the label, or is it derived from the RFID tag's EPC memory bank? This determines whether it is scanned from the tag or entered separately. |
| OQ-03 | P1 | Confirm the default Category list for the toy domain. Proposed seed list: Ride-On, Board Game, Educational, Magic Car, Hot Wheels, Craft Kit, Soft Toy, Action Figure, Outdoor, Other. |
| OQ-04 | P2 | Should **Excess** items (scanned but not in master) be included in the exported CSV? If yes, as a separate section/tab or interleaved with a distinct status value? |
| OQ-05 | P2 | What is the maximum expected number of linked items (master records) on a single store device? Required for DB and performance sizing. |
| OQ-06 | P2 | For this deployment, should **Price** be configured as a domain-specific field shown in the inventory results and CSV (Section 1.7), or only used during linking/product master? |
| OQ-07 | P2 | Will the same C72 device be used across multiple branches/stores (Crazy Hamster has a franchise model), or is each device permanently assigned to one store? This affects whether a store/location context is needed in session records. |
| OQ-08 | P3 | Should the exported CSV filename be auto-generated from Inventory Code + timestamp, or should the user be prompted to enter a custom filename at export time? |
| OQ-09 | P3 | Is an **Assets** module required in a future version? If yes, what entity types would it track (store fixtures, display units, staff equipment)? |
| OQ-10 | P1 | **API upload spec:** What is the target API endpoint for CSV upload? What authentication mechanism is required (API key header, Bearer token, Basic Auth)? What HTTP response indicates success? Is there a specific request format expected (multipart/form-data, raw body, JSON wrapper)? |
| OQ-11 | P1 | **LAN transfer protocol:** What is the preferred LAN delivery mechanism — SMB shared folder, FTP, or HTTP POST to a local server? What are the typical network credentials and destination path format used in the store environment? |
| OQ-12 | P2 | Should the app support **automatic delivery** of the CSV to API/LAN immediately after STOP (without user manually triggering export), or always require explicit user action? |
| OQ-13 | P1 | **Licensing unit:** Is the billing model per-device (one license token per C72 unit), per-store (one token covers all devices in a store), or per-franchise-location? This determines the `deviceId` binding strategy — single device ID vs a list of allowed device IDs in the token payload. |
| OQ-14 | P1 | **License duration:** Is the license perpetual (one-time purchase, no expiry) or subscription-based (annual/monthly renewal)? If subscription, confirm the preferred warning lead time before expiry (proposed: 14 days and 7 days). |
| OQ-15 | P1 | **SKU catalogue size:** How many unique product types (SKUs) does a typical Crazy Hamster franchise store carry? Do all franchise stores carry the same catalogue, or does each store stock a different subset? *(Drives DB sizing and Bulk CSV import performance targets.)* |
| OQ-16 | P1 | **Total linked items per device:** Roughly how many individual RFID-tagged toy units are physically present in a single store at any time — shop floor and stockroom combined? *(This is the single largest driver of DB size and scan result computation time.)* |
| OQ-17 | P1 | **Inventory session frequency:** How often is a stock-take expected to be run — daily, weekly, or only for periodic audits? Would multiple sessions ever be run in a single day (e.g. one per floor section or shelf zone)? *(Drives session data accumulation rate and storage sizing.)* |
| OQ-18 | P2 | **Session retention on device:** How far back does a store manager need to access past sessions on the device itself? Once a session's CSV has been sent to head office, can the raw session data on the device be purged — or must it be retained locally regardless? *(Determines whether auto-purge is acceptable and what the retention window should be.)* |
| OQ-19 | P2 | **Central repository for CSVs:** When the store sends an inventory CSV — where does it go? Is there a central system at head office (ERP, shared drive, email inbox) that receives and stores all CSVs from all franchise stores? *(If a central copy exists, on-device retention is less critical.)* |
| OQ-20 | P2 | **GST / audit retention requirement:** Does the client's CA or auditor require inventory records to be retained for a minimum period? Is the exported CSV sufficient for audit purposes, or is a more detailed record needed? *(Determines whether the 90-day default session retention window is adequate or needs extending.)* |
| OQ-21 | P2 | **Device loss / damage recovery expectation:** If the C72 was lost or damaged tomorrow, what is the expected recovery time — and who would perform it? Would the store be able to tolerate starting from scratch (re-linking all items), or is full restore to a replacement device a hard requirement? *(Determines how prominently the backup feature must be surfaced and how automated it must be.)* |
| OQ-22 | P2 | **Device reassignment across stores:** If a franchise store closes or a new one opens, would the C72 be reassigned to the new store — carrying data from the previous store? Or does each device remain permanently assigned to one store? *(Determines whether a "wipe and reassign" workflow is needed and whether sessions need a store/location tag.)* |
| OQ-23 | P2 | **Device management responsibility:** Who is the most technically responsible person for the C72 at a typical Crazy Hamster franchise — a dedicated IT person, the store manager, or the owner? *(Determines how automated the backup must be — a technically confident user can follow a manual process; a non-technical owner needs zero-effort automation.)* |
| OQ-24 | P2 | **Wi-Fi availability at stores:** Does every franchise store have a Wi-Fi router? Is it reliably available during business hours? Is the C72 already connected to store Wi-Fi for CSV upload purposes? *(Determines viability of Tier 3 automatic remote backup.)* |
| OQ-25 | P3 | **Back-office PC or NAS:** Is there a computer or NAS device in the store's back office that is on during business hours — for example, a billing PC? *(If yes, Tier 3 backup can target it over LAN using the same channel as CSV delivery, at zero additional infrastructure cost.)* |
| OQ-26 | P3 | **Existing cloud storage usage:** Does the store owner or manager currently use Google Drive, Dropbox, or any similar cloud service — even personally? *(If yes, the Tier 2 share-sheet backup is immediately usable with no new account setup.)* |
| OQ-27 | P3 | **SD card availability on C72:** Does the deployed C72 device have an external SD card installed? *(Tier 1 local backup prefers SD card over internal storage to isolate backups from app data. If no SD card is present, internal storage is used as fallback.)* |
| OQ-28 | P2 | **Security questions:** Confirm the set of security questions to offer during Admin first-run setup. Proposed set: "What is the name of your first school?", "What is your mother's maiden name?", "What was the name of your first pet?", "What is the name of your hometown?", "What was your childhood nickname?" — or should these be customisable? |
| OQ-29 | P2 | **Operator PIN length:** Confirm the minimum and maximum Operator PIN length. Proposed: minimum 4 digits, maximum 8 digits. Should the app also support alphanumeric Operator credentials in future, or remain numeric-only? |
| OQ-30 | ~~P1~~ **RESOLVED (v3.7)** | ~~**Article ID presence and uniqueness (critical):** Does the client's product labeling include a pre-printed Article ID per physical unit?~~ **Resolved:** Article ID is now a deployment-configurable fixed field (Not Used / Optional / Mandatory — Section 1.7, NFR-56), never used as a uniqueness or matching key. RFID Tag ID is the sole guaranteed unique per-unit identifier; Barcode is the product-level grouping/matching key. See Section 1.7 and FR-09c, FR-21, FR-22, FR-45, FR-77o. |
| OQ-31 | P1 | **Physical hardware trigger button:** Does the deployed C72 unit have a physical trigger/side button for UHF scan and imager activation (common on this class of rugged handheld), and if so, can it be hooked into the same scan action as the on-screen Scan/START button (NFR-10c, NFR-10d)? If a hardware trigger exists and is wired up, it becomes the primary one-handed scan mechanism in practice, which changes how much ergonomic weight the on-screen button's thumb-zone placement needs to carry — the software button remains necessary as a fallback and for accessibility, but the hardware trigger would be the expected primary path for repeated rapid scans. |

---

## 8. Document Revision History

| Version | Date | Description |
|---------|------|-------------|
| 0.1 | 03-Jun-2026 | Initial client discussion notes — Crazy Hamster Toy Store |
| 1.0 | Jun-2026 | First draft SRS from client notes |
| 1.1 | Jun-2026 | Refined Section 3.4: QR-driven stock receiving, GRN concept, bulk quantity support |
| 2.0 | Jun-2026 | Major revision: scope narrowed to 5 features; local-only storage; Linking module added; reference app screenshots incorporated. Domain incorrectly set to jewellery — corrected in v2.1. |
| 2.1 | Jun-2026 | Domain corrected to toy retail (Crazy Hamster, Yavatmal). Product master schema updated to toy fields: Name, Article ID, Barcode, Price, Category. Jewellery fields (Purity, Net/Gross Weight) removed. Category seed list updated for toy domain. Business context section added. |
| 2.2 | Jun-2026 | Scope updated: Report Export expanded to three delivery channels (Share sheet, API upload, LAN transfer). FR-41–FR-48 added. NFR-09 updated. OQ-10–12 added. |
| 2.3 | Jun-2026 | Section 4.1.1 Individual Linking rewritten: QR scan is sole input mechanism. FR-01–FR-11 rewritten; FR-12–FR-17 renumbered. OQ-01 sharpened. |
| 2.4 | Jun-2026 | FR-09 split into FR-09, FR-09a, FR-09b: Category master list; QR category validation with mismatch resolution; bulk Category CSV upload. |
| 2.5 | Jun-2026 | Added Section 4.1.3 Product Master Management (Option C): auto-upsert on QR scan, bulk CSV upload, product master view/edit/delete/export. Updated Module Overview table. |
| 2.6 | Jun-2026 | Category filter clarified as post-scan display filter: FR-23, FR-27, FR-34, FR-38 updated. Design constraint added. |
| 2.7 | Jun-2026 | Added Section 5.8 Licensing (server-based model): NFR-31–NFR-37. NFR-15 updated. Licensing design constraints and OQ-13–OQ-16 added. |
| 2.8 | Jun-2026 | Licensing model changed to fully offline cryptographic signing (RSA/ECDSA). NFR-31–NFR-38 rewritten. NFR-15 updated. Design constraints updated. OQ-15/16 removed. |
| 2.9 | Jun-2026 | Added Section 5.9 Backup and Restore (three-tier: NFR-39–NFR-50). Section 5.4 expanded (NFR-19a, NFR-19b). Backup design constraints and OQ-15–OQ-27 added. |
| 3.0 | Jun-2026 | Licensing updated to hybrid offline/lightweight-online model. Token ID added. Revocation list (NFR-37, NFR-37a–c) added. Device loss/bluffing scenario documented. NFR-15 and design constraints updated. |
| 3.1 | Jun-2026 | Added Section 5.5 Authentication, User Management, and RBAC. NFR-28 updated: language changed from Kotlin to Java. |
| 3.2 | Jun-2026 | Application named **StockBuddy**. Document title, header, purpose (1.1), and definitions table updated. Stale v2.1 change summary note removed. |
| 3.3 | Jun-2026 | Vendor name removed from document header, purpose statement, and definitions table. |
| 3.4 | Jun-2026 | Section 4.1.1 Individual Linking updated: manual entry mode added alongside QR scan mode — Barcode supports type or imager scan; RFID supports type or UHF Scan button; partial QR fallback switches to manual mode pre-populated. FRs renumbered throughout 4.1 (FR-01–FR-28). Section 4.4 Assets added as in-scope top-level module (FR-49–FR-56): flat linked-item list, search, category filter, delete Admin-only (linked item only, product master unaffected). Module Overview and RBAC table updated. |
| 3.5 | Jun-2026 | Section 1.7 added: domain-agnostic data model (fixed vs. configurable domain-specific fields, NFR-51–NFR-55), relocated from earlier draft 5.10; multiple FRs (FR-02, FR-05, FR-16, FR-17, FR-21, FR-22, FR-26, FR-32, FR-38, FR-56) and design constraints updated for consistency. FR-33 extended with group-expansion behaviour; OQ-30 added (Article ID uniqueness). New Section 4.5 Settings, Setup, and Administration added (FR-57–FR-68): Settings screen, Category Management, consolidated first-run setup flow (license activation + Admin setup + initial categories); single-Admin/shared-Operator-PIN model confirmed, Superadmin concept dropped in favour of Admin-accessible license renewal. Module Overview updated with Settings row. New Section 4.6 Functional Flow and Navigation added: full screen inventory (S01–S19), navigation map, and screen-by-screen detail, merged and reconciled with FR-57–68 (User Management/S17 and Superadmin/S20 from earlier draft removed; replaced by Settings sub-items and first-run wizard). |
| 3.6 | Jun-2026 | First-Run Setup Flow (FR-65–68, Section 4.5.3) expanded into four detailed, non-skippable steps: License Activation (FR-65a–d), Admin Account Setup (FR-65e–h), **Domain Field Configuration (FR-65i–n, new)**, and Initial Category Setup (FR-66, FR-66a). Added FR-68a and new Settings screen S20 (Field Configuration) for post-setup reconfiguration of domain-specific fields, reusing the Step 3 interface. Settings menu (FR-58), S16 detail, screen inventory, and navigation map updated accordingly. |
| 3.7 | Jun-2026 | **Article ID reclassified from always-on mandatory fixed field to deployment-configurable fixed field** (Section 1.7), with three modes: Not Used / Optional / Mandatory (default: Optional), set via new NFR-56 and new wizard step control FR-65o. RFID Tag ID confirmed as the sole guaranteed unique per-unit identifier (FR-09c); Barcode confirmed as the key used for Product Master matching and result grouping in place of Article ID (FR-21, FR-22, FR-26 updated; FR-33 rewritten). FR-09 mandatory-field list updated to drop Article ID. FR-02, FR-05, FR-16, FR-23, FR-25, FR-32, FR-38, FR-50, FR-51, FR-54, FR-56, FR-68a, screen details (S06, S08, S09), and the domain-agnostic schema design constraint updated for consistency. OQ-30 resolved and closed out. |
| 3.8 | Jun-2026 | **Requirement ID collision fixed.** Section 4.2 (Inventory — Stock Taking) had reused requirement IDs FR-17–FR-36, duplicating IDs already assigned to Bulk Linking (FR-17–FR-20) and Product Master Management (FR-21–FR-28) in Section 4.1 — a leftover from incremental edits across v2.3–v3.5 that was never caught. All requirements from the start of Section 4.2 (Inventory Code prompt) through the end of Section 4.5 (Domain field reconfiguration) renumbered to a single contiguous, collision-free sequence: **FR-29 through FR-80a**. No requirement content changed — IDs only. Every cross-reference throughout the document (Section 1.7, OQ-30, screen inventory table, screen-by-screen detail in Section 4.6) updated to the new IDs. FR-09a/FR-09b/FR-09c reordered into alphabetical sequence (was 09, 09c, 09a, 09b). Historical revision history entries (v0.1–v3.7) left unchanged, as they describe the document's numbering at the time each entry was written. |
| 3.9 | Jun-2026 | **Download (save-to-device) formalised as a fourth Report Export channel**, alongside Share, API upload, and LAN transfer. Section 1.2 (Feature 5) updated from three to four channels. FR-49 updated to list Download as a choice. New **FR-52a** added specifying save-to-device behaviour (location picker, save confirmation with file path, option to chain into Share). This closes a gap where screen detail S14 referenced a "save-to-device" behaviour inherited from the reference app, but no formal FR ever specified it — S14's detail text corrected accordingly (was inconsistently describing four channels as "three"). |
| 3.10 | Jun-2026 | **Hardware connection status formalised.** The "Reader Connected" status bar element (Section 4.6.2) was previously prose-only with no formal requirement. New **FR-81** specifies a three-state indicator (Connected / Not Connected / Not Available, the latter covering non-C72 devices and emulators). New **FR-81a** specifies graceful degradation: RFID/imager-dependent actions disable with a clear message rather than failing silently or crashing, while all non-hardware-dependent features remain fully usable. New **NFR-26a** (Section 5.6) requires the app to run without crashing on an Android emulator or any device lacking the RFID SDK, enabling UI/UX and non-hardware feature testing without a physical C72 for every cycle — actual scan/imager hardware verification still required on a physical device before release. |
| 3.11 | Jun-2026 | **UI/UX requirements formalised** in Section 5.3 (Usability), replacing a single thin Design Constraints bullet. New **NFR-10a** (Material Design 3 as the design system), **NFR-10b** (6-inch rugged-handheld form factor), **NFR-10c** (primary actions ≥56dp, larger than the general touch-target baseline), **NFR-10d** (thumb-zone placement for Scan/START/STOP on the lower half of the screen, for one-handed operation), **NFR-10e** (one primary action per screen, secondary detail deferred). Design Constraints (Section 6) MD3 bullet updated to point at the new formal NFRs instead of duplicating them. New **OQ-31** added: whether the C72 unit has a physical hardware trigger button, which affects how much ergonomic weight the on-screen button's thumb-zone placement needs to carry in practice. |
| 3.12 | Jun-2026 | **Field-level scan affordances clarified against the primary-action rule.** New **NFR-10f** defines Barcode scan (FR-06) and RFID Tag ID scan (FR-07) as field-level utility actions exempt from NFR-10e's one-primary-action rule — Barcode scan as a compact inline icon, RFID Tag ID scan as a more prominent labelled button given its central role in uniqueness validation (FR-08) and richer feedback states (FR-07a). Save Link remains the screen's single oversized, thumb-zone primary action (NFR-10c/10d), distinct from both. NFR-10e updated to reference the exception. No change to FR-06/FR-07 themselves — this was already specified at the requirement level; the gap was purely in the visual-hierarchy guidance. |
| 3.13 | Jun-2026 | **Feedback message visibility formalised.** New **NFR-13a** (Section 5.3) requires success, error, and confirmation messages to be visually prominent enough to notice in bright showroom lighting, not rely on colour alone (paired with icon/text, extending NFR-11), persist appropriately (critical messages stay until acknowledged; transient ones stay readable before dismissing), and avoid being obscured by the keyboard or the user's hand during one-handed use (ties to NFR-10d). Distinct from NFR-13, which governs message *language* (plain language, no stack traces) rather than *presentation*. |
| 3.14 | Jun-2026 | **Pre-implementation decisions locked in.** NFR-28 language strategy reversed back to **Kotlin (primary) + Java (Chainway SDK integration and any Java-only third-party libraries)** — this undoes the Java-only constraint set in v3.1. New Design Constraints added: Application ID `com.gigakin.stockbuddy`; confirmed from-scratch build with no source code ported from the reference application (its screenshots were UI/UX reference only, per Section 4.6.1). Build/tooling decisions not requiring an SRS change (CSV library default, no CI/CD for this phase, physical C72 available on a need basis but guaranteed before release, signing/distribution approach) are tracked in the companion MVP/Demo Scope document instead, since they're delivery-process decisions rather than product requirements. |
| 3.15 | Jul-2026 | **Data model v2 finalised and reflected in scope.** The MVP data model was implemented as a two-layer design — **normalized master + immutable denormalized session snapshot** — authoritatively specified in `docs/design/StockBuddy_System_Design.md` §4.0. Requirement-level consequences recorded here: (a) **Product-level attributes** — domain-specific fields (Section 1.7) live on the product (Barcode), shared by all its tagged units; there are no per-unit custom fields. (b) **Category is a first-class referenced entity** — linking/import reject an unknown category rather than auto-creating it; deleting a referenced category is block-and-warn then cascade (FR-74). (c) **RFID Tag ID uniqueness (FR-08/NFR-18)** is enforced as the primary key of the unit record, so a tag binds to exactly one Barcode — this subsumes any RFID+Barcode composite-uniqueness requirement. (d) **Results are read from a snapshot** captured at STOP (Section 4.2/4.3), so a reopened historical session reflects product data as it was at scan time and does not drift when the master is later edited; Available/Missing/**Excess** (FR-40–42) are grouped by Barcode **per tab**, with per-group counts and RFID-level expansion (FR-45). (e) **Bulk import is append-only** (FR-17/18): product insert-or-reject-on-name-mismatch; unit insert-if-new-else-skip; summary reports Inserted/Skipped/Rejected. (f) **Article ID dropped as a fixed field** — the v3.7 3-mode toggle was never implemented and is superseded; **FR-77o, NFR-56, FR-09c, FR-65o are void** and the Article-ID mode control in FR-80a is removed (Section 1.7 annotated). An Article ID identifier, if wanted, is now just a configurable domain-specific field (NFR-51–54) in the product `attributes`; Barcode is the sole business key. MVP scope inclusions (Excess tab, product-level Assets with edit/delete, category delete-warning) and demo-limit values (200/50/100) are tracked in the companion MVP/Demo Scope document. No FRs renumbered. |
| 3.17 | Jul-2026 | **QR Code Linking (S07) implemented — imager-based.** The QR scanning screen is now built and wired: Linking Options → **QR Code Linking (S07)** → on a successful decode, **Individual Linking (S06)** opens **pre-filled** from the payload (FR-02/03) for review, then Save (reusing all S06 validation). Back from S06 returns to S07 (nav stack, no popUpTo); after a save that came via QR, S06 returns to S07 re-armed for the next item (**FR-11**). **Scanning uses the C72 2D imager** (`com.rscja.barcode.BarcodeDecoder` via `ScannerManager.openImager/triggerImagerScan/closeImager`), satisfying **NFR-24** — **not** the phone camera; the S07 design's camera viewfinder is rendered as a decorative "aim zone" (no live feed). **Device-only**: on the emulator the imager is unavailable, so S07 shows the FR-81a "reader unavailable" state (scan disabled). **OQ-01 resolved**: the payload is a fixed **JSON** schema (`util/QrPayload.kt`). Malformed QR → inline "unrecognized" + re-arm (**FR-04** partial handling: missing fields simply leave those inputs blank for the user to complete). Companion MVP/Demo Scope doc updated (QR scanning moved to in-scope); CLAUDE.md updated. Note: the older FR-01/02 framing of QR as the *sole* Individual-Linking input is superseded — QR is now **one of three** optional linking paths (QR / Manual / Bulk); no FRs renumbered. | The Linking Options screen (S04) now presents **QR Code Linking** as the first option alongside Individual (Manual) Linking and Bulk Linking, per the Linking Options design. The **QR scanning screen (S07) and its behaviour (FR-01–04, FR-11) remain unimplemented** — tapping the option shows a "coming soon" message, and Individual Linking is still manual-entry only. This moves QR from *fully cut* to **option-present, scanning-deferred** in the companion MVP/Demo Scope document (out-of-scope table + §1.3 UX note) and in CLAUDE.md. The QR payload schema (OQ-01) and the label-printing dependency remain open before S07 can be built; the `docs/html_screens/` QR scanning design will be added at that time. No FR content changed. |
