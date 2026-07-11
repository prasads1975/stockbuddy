package com.gigakin.stockbuddy.ui.linking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gigakin.stockbuddy.hardware.ScannerManager

/** S07 — arms the C72 2D imager and surfaces decoded QR strings (FR-01/02). */
class QrLinkingViewModel(private val scanner: ScannerManager) : ViewModel() {

    private val _decoded = MutableLiveData<String?>()
    val decoded: LiveData<String?> get() = _decoded

    private var opened = false

    /** Arms the imager (idempotent). Returns false if no imager is available (e.g. emulator). */
    fun openImager(): Boolean {
        if (!opened) {
            // Callback fires from an SDK thread — postValue is main-thread-safe.
            opened = scanner.openImager { data -> _decoded.postValue(data) }
            if (opened) scanner.triggerImagerScan()
        }
        return opened
    }

    /** Re-arm / software-trigger a scan (FR-11 rapid re-scan; the hardware trigger also works). */
    fun triggerScan() { if (opened) scanner.triggerImagerScan() }

    fun closeImager() {
        if (opened) { scanner.closeImager(); opened = false }
    }

    fun consumeDecoded() { _decoded.value = null }

    override fun onCleared() { closeImager() }
}
