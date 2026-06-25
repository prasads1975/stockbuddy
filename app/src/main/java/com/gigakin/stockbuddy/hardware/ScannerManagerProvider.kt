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
            // TODO: once the real SDK is wired in, detect non-presence here (e.g. SDK init
            // throws, or a class-not-found check) and fall through to EmulatorScannerManager
            // instead of returning ChainwayScannerManager unconditionally.
            ChainwayScannerManager()
        } catch (e: Throwable) {
            EmulatorScannerManager()
        }
}
