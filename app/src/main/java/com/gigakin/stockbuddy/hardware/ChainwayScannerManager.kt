package com.gigakin.stockbuddy.hardware

import androidx.lifecycle.MutableLiveData
import com.gigakin.stockbuddy.util.ReaderStatus

/**
 * Real C72 hardware implementation — PLACEHOLDER pending the actual Chainway SDK.
 *
 * Per the build-setup notes (Section 7, MVP Scope doc): once app/libs/chainway-sdk.aar is
 * in place and the dependency is uncommented in app/build.gradle.kts, wire the TODOs below
 * to the SDK's actual UHF/imager APIs. Until then, ScannerManagerProvider falls back to
 * EmulatorScannerManager automatically (see below) so the rest of the app is unaffected.
 *
 * Expected shape based on typical Chainway SDK patterns (CONFIRM against actual SDK docs):
 *  - A reader service/manager singleton, usually opened in Application.onCreate() and
 *    closed in onTerminate() (or per-Activity lifecycle — check the SDK sample app).
 *  - UHF inventory callbacks delivering EPC hex strings as tags are read.
 *  - A "power level" setting relevant to FR-07a's single-shot/short-range scan behaviour.
 */
class ChainwayScannerManager : ScannerManager {
    override val status = MutableLiveData(ReaderStatus.NOT_CONNECTED)

    init {
        // TODO: initialise the Chainway UHF reader handle here, e.g.
        //   uhfReader = RFIDWithUHFUART.getInstance()
        //   uhfReader.init(context)
        // On success: status.value = ReaderStatus.CONNECTED
        // On failure: status.value = ReaderStatus.NOT_CONNECTED
    }

    override suspend fun scanSingleRfidTag(): RfidScanResult {
        // TODO: FR-07a — single-shot inventory burst at minimum transmit power, then
        // inspect the result set: empty -> NoTagDetected, 1 -> Success(epc),
        // 2+ -> MultipleTagsDetected(count). Restore session-default power afterward
        // regardless of outcome (FR-07a).
        return RfidScanResult.ReaderUnavailable
    }

    override fun startContinuousScan(onTagRead: (String) -> Unit) {
        // TODO: start the SDK's continuous inventory mode, forwarding each EPC read to
        // onTagRead(epc). FR-37's dedup is handled at the repository/DB layer (DAO uses
        // OnConflictStrategy.IGNORE), so this callback can fire on every physical read.
    }

    override fun stopContinuousScan() {
        // TODO: stop the SDK's continuous inventory mode.
    }

    override suspend fun scanBarcode(): String? {
        // TODO: FR-06 — invoke the C72 imager via the SDK's barcode/QR scan API.
        return null
    }
}
