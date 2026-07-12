package com.gigakin.stockbuddy.hardware

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.gigakin.stockbuddy.util.ReaderStatus
import com.rscja.barcode.BarcodeDecoder
import com.rscja.barcode.BarcodeFactory
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.interfaces.ConnectionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Real C72 hardware implementation using Chainway DeviceAPI SDK.
 * Integrates with RFIDWithUHFUART for UHF RFID scanning.
 *
 * Uses the actual Chainway SDK API:
 * - RFIDWithUHFUART.getInstance() — reader singleton
 * - init(context) — initialize with Android context
 * - inventorySingleTag() — single-tag inventory burst
 * - startInventoryTag() / setInventoryCallback() / stopInventory() — continuous mode
 * - setPower() / power property — TX power control for FR-07a
 * - setConnectionStatusCallback() / getConnectStatus() — live connection status (FR-81), so a
 *   mid-session disconnect repaints the status bar without polling.
 */
class ChainwayScannerManager(private val context: Context) : ScannerManager {
    override val status = MutableLiveData(ReaderStatus.NOT_CONNECTED)

    private var uhfReader: RFIDWithUHFUART? = null
    private var isScanning = false
    private var lastSessionPowerLevel: Int? = null

    private var barcodeDecoder: BarcodeDecoder? = null

    init {
        try {
            uhfReader = RFIDWithUHFUART.getInstance()
            val initialized = uhfReader?.init(context) ?: false
            status.value = if (initialized) ReaderStatus.CONNECTED else ReaderStatus.NOT_CONNECTED
            if (initialized) registerConnectionStatusListener()
        } catch (e: Exception) {
            status.value = ReaderStatus.NOT_CONNECTED
            uhfReader = null
        }
    }

    /**
     * FR-81: drive the status LiveData from the SDK's connection state so a disconnect that happens
     * mid-session (not just at startup) is reflected on whatever screen is showing. The callback is
     * event-driven (no polling); it may fire off the main thread, hence postValue. CONNECTING is
     * treated as NOT_CONNECTED — the reader isn't usable yet, so scan actions stay disabled (FR-81a).
     * Also seeds the current state once via getConnectStatus() in case a change already happened.
     */
    private fun registerConnectionStatusListener() {
        val reader = uhfReader ?: return
        try {
            reader.setConnectionStatusCallback { connStatus, _ ->
                status.postValue(mapConnectionStatus(connStatus))
            }
            reader.connectStatus?.let { status.postValue(mapConnectionStatus(it)) }
        } catch (e: Exception) {
            // Older SDK builds may not support the connection callback; the startup status still
            // applies and continuous-scan failures still flip to NOT_CONNECTED as a fallback.
        }
    }

    private fun mapConnectionStatus(connStatus: ConnectionStatus?): ReaderStatus =
        if (connStatus == ConnectionStatus.CONNECTED) ReaderStatus.CONNECTED else ReaderStatus.NOT_CONNECTED

    override suspend fun scanSingleRfidTag(): RfidScanResult = suspendCancellableCoroutine { continuation ->
        try {
            val reader = uhfReader ?: run {
                continuation.resume(RfidScanResult.ReaderUnavailable)
                return@suspendCancellableCoroutine
            }

            // FR-07a: save power level, lower to minimum for short-range single-shot
            lastSessionPowerLevel = try { reader.power } catch (e: Exception) { 27 }
            try { reader.setPower(18) } catch (e: Exception) {}

            // FR-07: perform single inventory operation
            val tag = try {
                reader.inventorySingleTag()
            } catch (e: Exception) {
                null
            }

            // Restore power level regardless of outcome (FR-07a)
            lastSessionPowerLevel?.let { try { reader.setPower(it) } catch (e: Exception) {} }

            val result = when {
                tag == null -> RfidScanResult.NoTagDetected
                else -> RfidScanResult.Success(tag.epc)
            }
            continuation.resume(result)
        } catch (e: Exception) {
            lastSessionPowerLevel?.let {
                try { uhfReader?.setPower(it) } catch (ex: Exception) {}
            }
            continuation.resume(RfidScanResult.ReaderUnavailable)
        }
    }

    override fun startContinuousScan(onTagRead: (String) -> Unit) {
        try {
            val reader = uhfReader ?: return
            if (isScanning) return

            isScanning = true

            // FR-37: set session-default power for continuous mode
            try { reader.setPower(27) } catch (e: Exception) {}

            // Start continuous inventory and set callback
            try {
                reader.startInventoryTag(null)
                reader.setInventoryCallback { tag ->
                    try {
                        onTagRead(tag.epc)
                    } catch (e: Exception) {
                        // Ignore malformed tag; continue reading
                    }
                }
            } catch (e: Exception) {
                isScanning = false
                status.value = ReaderStatus.NOT_CONNECTED
            }
        } catch (e: Exception) {
            isScanning = false
        }
    }

    override fun stopContinuousScan() {
        try {
            isScanning = false
            uhfReader?.stopInventory()
        } catch (e: Exception) {
            // Silent fail; reader may already be stopped
        }
    }

    override suspend fun scanBarcode(): String? = withTimeoutOrNull(10000) {
        suspendCancellableCoroutine { continuation ->
            try {
                // FR-06: invoke C72 imager for 1D barcode scan (EAN-13, Code128, etc.)
                // Uses same BarcodeDecoder API as QR; handles both symbologies.
                val decoder = BarcodeFactory.getInstance().barcodeDecoder
                val opened = decoder.open(context)
                if (!opened) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                decoder.setDecodeCallback { entity ->
                    try {
                        if (entity != null && entity.resultCode == BarcodeDecoder.DECODE_SUCCESS && continuation.isActive) {
                            val data = entity.barcodeData
                            if (!data.isNullOrBlank()) {
                                decoder.stopScan()
                                decoder.close()
                                continuation.resume(data)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore; keep imager armed for next attempt.
                    }
                }

                decoder.startScan()

                continuation.invokeOnCancellation {
                    try {
                        decoder.stopScan()
                        decoder.close()
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    // FR-01/02: 2D imager (QR) via com.rscja.barcode.BarcodeDecoder.
    override fun openImager(onDecoded: (String) -> Unit): Boolean {
        return try {
            val decoder = BarcodeFactory.getInstance().barcodeDecoder
            val opened = decoder.open(context)
            if (opened) {
                decoder.setDecodeCallback { entity ->
                    try {
                        if (entity != null && entity.resultCode == BarcodeDecoder.DECODE_SUCCESS) {
                            val data = entity.barcodeData
                            if (!data.isNullOrBlank()) onDecoded(data)
                        }
                    } catch (e: Exception) {
                        // Ignore a malformed decode result; keep the imager armed.
                    }
                }
                barcodeDecoder = decoder
            }
            opened
        } catch (e: Exception) {
            barcodeDecoder = null
            false
        }
    }

    override fun triggerImagerScan() {
        try { barcodeDecoder?.startScan() } catch (e: Exception) {}
    }

    override fun closeImager() {
        try {
            barcodeDecoder?.stopScan()
            barcodeDecoder?.close()
        } catch (e: Exception) {
            // Silent fail; imager may already be closed.
        }
        barcodeDecoder = null
    }

    fun close() {
        try {
            uhfReader?.free()
        } catch (e: Exception) {
            // Silent fail
        }
        closeImager()
        uhfReader = null
        isScanning = false
    }
}
