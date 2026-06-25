package com.gigakin.stockbuddy.ui.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.data.repo.InventoryRepository
import com.gigakin.stockbuddy.hardware.ScannerManager
import kotlinx.coroutines.launch

/** FR-33-37: START/STOP scanning, real-time dedup count, category filter (display-only). */
class ScanningViewModel(
    private val inventoryRepository: InventoryRepository,
    private val categoryRepository: CategoryRepository,
    private val scannerManager: ScannerManager
) : ViewModel() {

    val categories = categoryRepository.observeAll()

    private val _scanCount = MutableLiveData(0)
    val scanCount: LiveData<Int> get() = _scanCount

    private val _scanning = MutableLiveData(false)
    val scanning: LiveData<Boolean> get() = _scanning

    fun start(sessionId: Long) {
        _scanning.value = true
        scannerManager.startContinuousScan { tag ->
            viewModelScope.launch {
                inventoryRepository.recordScan(sessionId, tag)
                _scanCount.value = inventoryRepository.distinctScanCount(sessionId)
            }
        }
    }

    fun stop(sessionId: Long) = viewModelScope.launch {
        scannerManager.stopContinuousScan()
        _scanning.value = false
        inventoryRepository.stopSession(sessionId)
    }
}
