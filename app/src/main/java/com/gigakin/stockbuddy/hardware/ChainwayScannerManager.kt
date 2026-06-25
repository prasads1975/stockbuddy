package com.gigakin.stockbuddy.hardware

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.gigakin.stockbuddy.util.ReaderStatus
import com.rscja.deviceapi.RFIDWithUHFUART
import kotlinx.coroutines.suspendCancellableCoroutine
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
 */
class ChainwayScannerManager(private val context: Context) : ScannerManager {
    override val status = MutableLiveData(ReaderStatus.NOT_CONNECTED)

    private var uhfReader: RFIDWithUHFUART? = null
    private var isScanning = false
    private var lastSessionPowerLevel: Int? = null

    init {
        try {
            uhfReader = RFIDWithUHFUART.getInstance()
            val initialized = uhfReader?.init(context) ?: false
            status.value = if (initialized) ReaderStatus.CONNECTED else ReaderStatus.NOT_CONNECTED
        } catch (e: Exception) {
            status.value = ReaderStatus.NOT_CONNECTED
            uhfReader = null
        }
    }

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

    override suspend fun scanBarcode(): String? = suspendCancellableCoroutine { continuation ->
        try {
            if (uhfReader == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            // FR-06: invoke C72 imager for barcode/QR scan
            // SDK barcode scanning API is typically image-capture based; placeholder
            // until barcode API is confirmed in SDK documentation or reference project.
            val barcode: String? = null

            continuation.resume(barcode)
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    fun close() {
        try {
            uhfReader?.free()
        } catch (e: Exception) {
            // Silent fail
        }
        uhfReader = null
        isScanning = false
    }
}
