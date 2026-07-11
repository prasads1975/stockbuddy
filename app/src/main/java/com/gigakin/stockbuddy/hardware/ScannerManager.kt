package com.gigakin.stockbuddy.hardware

import androidx.lifecycle.LiveData
import com.gigakin.stockbuddy.util.ReaderStatus

/**
 * FR-81/81a, NFR-23, NFR-26a: hardware abstraction over the C72's UHF RFID and barcode
 * imager modules. All SDK calls are wrapped behind this interface (per NFR-27's repository
 * pattern) so the rest of the app never talks to the Chainway SDK directly — this is what
 * makes graceful emulator degradation (FR-81a) a one-place concern instead of a try/catch
 * scattered across every screen.
 *
 * Two implementations:
 *  - EmulatorScannerManager: always reports NOT_AVAILABLE, used automatically when the
 *    real SDK can't be initialised (e.g. running on an Android emulator). See FR-81a.
 *  - ChainwayScannerManager: wraps the real SDK once it's dropped into app/libs/.
 */
interface ScannerManager {
    val status: LiveData<ReaderStatus>

    /** UHF RFID single-shot scan, per FR-07a's single-tag enforcement during Linking. */
    suspend fun scanSingleRfidTag(): RfidScanResult

    /** Continuous UHF scanning for an Inventory session (FR-33/34/37). Emits each unique tag read. */
    fun startContinuousScan(onTagRead: (String) -> Unit)
    fun stopContinuousScan()

    /** Barcode/QR imager scan (FR-06). */
    suspend fun scanBarcode(): String?

    /**
     * FR-01/02: 2D imager (QR) lifecycle for the QR Code Linking screen (S07).
     * [openImager] arms the C72 imager and registers [onDecoded] for each successful read
     * (the decoded string is delivered from an SDK thread — callers must marshal to the main
     * thread). Returns false when no imager is available (e.g. emulator) so the screen can show
     * the FR-81a unavailable state. [triggerImagerScan] fires a software scan (the C72 hardware
     * trigger also delivers to [onDecoded]); [closeImager] releases it.
     */
    fun openImager(onDecoded: (String) -> Unit): Boolean
    fun triggerImagerScan()
    fun closeImager()
}

sealed class RfidScanResult {
    data class Success(val epc: String) : RfidScanResult()
    object NoTagDetected : RfidScanResult()
    data class MultipleTagsDetected(val count: Int) : RfidScanResult()
    object ReaderUnavailable : RfidScanResult()
}
