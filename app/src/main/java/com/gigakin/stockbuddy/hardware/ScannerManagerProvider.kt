package com.gigakin.stockbuddy.hardware

import android.content.Context

/**
 * NFR-26a: chooses the real Chainway implementation when the SDK is present and able to
 * initialise, and falls back to the Emulator implementation otherwise (covers running on
 * an Android emulator, or any device without the SDK/hardware) — without crashing.
 */
object ScannerManagerProvider {
    @Volatile private var instance: ScannerManager? = null

    fun get(context: Context): ScannerManager =
        instance ?: synchronized(this) {
            instance ?: create(context).also { instance = it }
        }

    private fun create(context: Context): ScannerManager =
        try {
            // Try to initialize real Chainway SDK; fall back to emulator if SDK unavailable
            // (e.g. running on Android emulator, SDK missing, or device not C72)
            ChainwayScannerManager(context)
        } catch (e: Throwable) {
            EmulatorScannerManager()
        }
}
