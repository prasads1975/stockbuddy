# StockBuddy — Sample RFID & Barcode Test Values

**Purpose:** Realistic-format values for typing into the Manual Entry form (Barcode and RFID Tag ID fields) while testing on the **emulator**, before physical tags/labels or a C72 device are in hand. These let you exercise validation, uniqueness checks, grouping, and the full link → scan-simulation → results flow without real hardware.

**Important — these are fictional, not real products:**
- Barcodes use a `000`-prefixed body (not a real GS1 company prefix) with a correctly computed EAN-13 check digit, so they pass standard format/checksum validation but are guaranteed not to collide with any real registered product.
- RFID values are 96-bit EPC-format hex strings (24 characters, `E280...` prefix — a common Gen2 EPC tag header range) generated randomly for this list. They are not allocated to any real tag.

Do not use these for anything beyond app testing.

---

## 20 Sample Barcodes (EAN-13 format, valid check digit)

```
0000265423514
0000391171822
0000838637942
0000957015430
0001043321817
0001615594076
0002388496963
0002691669788
0002782489639
0003010310510
0003103413166
0003315098397
0003503056413
0003953767242
0004752553418
0004801845143
0004893252881
0005328710129
0006270482812
0007376311655
```

## 20 Sample RFID Tag IDs (96-bit EPC, hex)

```
E28003241B4D419B1B673BD4
E28009CC6273931BDB2A0DF3
E2800FA5F6B8A880627DF7FF
E2801449273D7CEE9D913668
E280194F309FFEA518F32CF2
E280210760474F36E8B53593
E2802684B27B95E909348334
E2803712DA86A78C49EA20E3
E2805FE878F78E2978AA2447
E2806A3566F893697B590481
E280755D05AD7853C1F76EB9
E2807706CA828BCA0385813D
E280896A68F812D810A485ED
E280AC946DC59C0996DAEEE6
E280BAD3C681D06BD2AA399D
E280C462DDAED16DC0CF0B9C
E280D7F78DF0CAC5E40C02D4
E280DBE4D58FED8A728E7ECA
E280DDE131CA3766E4D58E72
E280DF469611A11F5125227C
```

---

## Suggested test scenarios using these values

| Scenario | How to use the lists |
|---|---|
| **Multiple units, same product (Barcode grouping)** | Pick one barcode (e.g. `0000265423514`) and pair it with 3–4 *different* RFID values from the list above. Link all four as separate items with the same Barcode/Name/Category but different RFID Tag IDs — confirms grouping (FR-45) clusters them correctly in results. |
| **RFID uniqueness validation (FR-08)** | Link an item using one RFID value, then try to link a second item reusing the *same* RFID value — confirms the duplicate-RFID error fires correctly. |
| **Bulk Linking CSV import** | Build a small CSV by hand combining these values with made-up Names/Categories — exercises FR-15–19 without needing a "real" dataset. |
| **Missing items in results** | Link 10 items (10 barcode+RFID pairs above), then only "scan" 7 of them during a test session (i.e. only reference 7 of the 10 RFID values when simulating scan results) — the other 3 should appear as Missing. |
| **Excess / unrecognized tag** | Reference an RFID value from this list that was never linked — it should appear as Excess (if that tab is built) or simply not match any record. |

When the physical C72 and real tags are available, swap to actually scanning real barcodes/RFID tags for the hardware-only verification pass (Section 6 of the MVP/Demo Scope doc) — these values are for UI/logic testing only, not a substitute for that pass.
