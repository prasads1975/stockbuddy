package com.gigakin.stockbuddy.ui.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.data.repo.InventoryRepository
import com.gigakin.stockbuddy.hardware.ScannerManager
import com.gigakin.stockbuddy.util.ReaderStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** FR-33-37: START/STOP scanning, real-time dedup count, category filter (display-only). */
class ScanningViewModel(
    private val inventoryRepository: InventoryRepository,
    private val categoryRepository: CategoryRepository,
    private val scannerManager: ScannerManager
) : ViewModel() {

    data class ScanStats(
        val available: Int = 0,
        val missing: Int = 0,
        val excess: Int = 0
    )

    val categories = categoryRepository.observeAll()

    private val _scanCount = MutableLiveData(0)
    val scanCount: LiveData<Int> get() = _scanCount

    private val _scanning = MutableLiveData(false)
    val scanning: LiveData<Boolean> get() = _scanning

    private val _scanStats = MutableLiveData(ScanStats())
    val scanStats: LiveData<ScanStats> get() = _scanStats

    private var simulationJob: Job? = null
    private var simulatedTagCounter = 0

    fun start(sessionId: String) {
        _scanning.value = true
        val readerStatus = scannerManager.status.value

        // If reader is available, use real scanning; otherwise use simulation mode
        if (readerStatus == ReaderStatus.CONNECTED) {
            startRealScanning(sessionId)
        } else {
            startSimulatedScanning(sessionId)
        }
    }

    private fun startRealScanning(sessionId: String) {
        scannerManager.startContinuousScan { tag ->
            viewModelScope.launch {
                inventoryRepository.recordScan(sessionId, tag)
                _scanCount.value = inventoryRepository.distinctScanCount(sessionId)
                updateScanStats(sessionId)
            }
        }
    }

    private fun startSimulatedScanning(sessionId: String) {
        simulatedTagCounter = 0
        simulationJob = viewModelScope.launch {
            while (isActive && _scanning.value == true) {
                delay(2000) // Simulate a tag every 2 seconds
                val fakeTag = "RFID_TAG_${System.currentTimeMillis()}_${simulatedTagCounter++}"
                inventoryRepository.recordScan(sessionId, fakeTag)
                _scanCount.value = inventoryRepository.distinctScanCount(sessionId)
                updateScanStats(sessionId)
            }
        }
    }

    fun stop(sessionId: String) = viewModelScope.launch {
        simulationJob?.cancel()
        simulationJob = null
        scannerManager.stopContinuousScan()
        _scanning.value = false
        inventoryRepository.stopSession(sessionId)
    }

    private fun updateScanStats(sessionId: String) = viewModelScope.launch {
        val repoStats = inventoryRepository.computeSessionStats(sessionId)
        _scanStats.value = ScanStats(
            available = repoStats.available,
            missing = repoStats.missing,
            excess = repoStats.excess
        )
    }
}
