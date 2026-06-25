package com.gigakin.stockbuddy.hardware

import androidx.lifecycle.MutableLiveData
import com.gigakin.stockbuddy.util.ReaderStatus

/**
 * NFR-26a: lets the app run fully on an Android emulator without crashing. Status always
 * reports NOT_AVAILABLE (FR-81), and scan calls return ReaderUnavailable rather than
 * throwing — every screen built against ScannerManager already handles this gracefully (FR-81a).
 *
 * For manual UI/logic testing without hardware, use the sample values in
 * StockBuddy_Test_Values_RFID_Barcode.md by typing them into the manual-entry fields directly —
 * this manager intentionally does NOT fabricate scan results, so "Scan" always means a real read.
 */
class EmulatorScannerManager : ScannerManager {
    override val status = MutableLiveData(ReaderStatus.NOT_AVAILABLE)

    override suspend fun scanSingleRfidTag(): RfidScanResult = RfidScanResult.ReaderUnavailable

    override fun startContinuousScan(onTagRead: (String) -> Unit) {
        // No-op: no hardware to read from. UI should already be disabled per FR-81a
        // when status == NOT_AVAILABLE, so this should not normally be reachable.
    }

    override fun stopContinuousScan() { /* no-op */ }

    override suspend fun scanBarcode(): String? = null
}
