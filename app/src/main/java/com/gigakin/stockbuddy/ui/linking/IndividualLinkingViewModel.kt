package com.gigakin.stockbuddy.ui.linking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.data.repo.FieldConfigRepository
import com.gigakin.stockbuddy.data.repo.ItemRepository
import com.gigakin.stockbuddy.hardware.RfidScanResult
import com.gigakin.stockbuddy.hardware.ScannerManager
import com.gigakin.stockbuddy.util.ArticleIdMode
import kotlinx.coroutines.launch

/** FR-05-10, FR-09a-c, NFR-51-56: backs the Individual Linking form. */
class IndividualLinkingViewModel(
    private val itemRepository: ItemRepository,
    private val fieldConfigRepository: FieldConfigRepository,
    private val categoryRepository: CategoryRepository,
    private val scannerManager: ScannerManager
) : ViewModel() {

    val fieldDefs: LiveData<List<FieldDefinitionEntity>> = fieldConfigRepository.observeFields()
    val categories: LiveData<List<CategoryEntity>> = categoryRepository.observeAll()
    val articleIdMode: ArticleIdMode get() = fieldConfigRepository.articleIdMode

    private val _saveResult = MutableLiveData<ItemRepository.SaveResult?>()
    val saveResult: LiveData<ItemRepository.SaveResult?> get() = _saveResult

    private val _rfidScanResult = MutableLiveData<RfidScanResult?>()
    val rfidScanResult: LiveData<RfidScanResult?> get() = _rfidScanResult

    /** FR-07/07a: single-shot UHF scan into the RFID field. */
    fun scanRfid() = viewModelScope.launch {
        _rfidScanResult.value = scannerManager.scanSingleRfidTag()
    }

    /** FR-06: imager scan into the Barcode field. */
    fun scanBarcode(onResult: (String?) -> Unit) = viewModelScope.launch {
        onResult(scannerManager.scanBarcode())
    }

    fun save(
        name: String, barcode: String, category: String, rfid: String,
        articleId: String?, attributes: Map<String, String>
    ) = viewModelScope.launch {
        _saveResult.value = itemRepository.saveLinkedItem(
            name, barcode, category, rfid, articleId, articleIdMode, attributes
        )
    }

    fun consumeSaveResult() { _saveResult.value = null }
    fun consumeRfidScanResult() { _rfidScanResult.value = null }
}
