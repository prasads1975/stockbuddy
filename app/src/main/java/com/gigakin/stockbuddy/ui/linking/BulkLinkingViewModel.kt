package com.gigakin.stockbuddy.ui.linking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.repo.FieldConfigRepository
import com.gigakin.stockbuddy.data.repo.ItemRepository
import com.gigakin.stockbuddy.util.CsvUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

/** FR-15-19: Bulk Linking via CSV import. */
class BulkLinkingViewModel(
    private val itemRepository: ItemRepository,
    private val fieldConfigRepository: FieldConfigRepository
) : ViewModel() {

    private val _result = MutableLiveData<ItemRepository.BulkImportResult?>()
    val result: LiveData<ItemRepository.BulkImportResult?> get() = _result

    fun importCsv(inputStream: java.io.InputStream) = viewModelScope.launch {
        val rows = withContext(Dispatchers.IO) { CsvUtils.read(InputStreamReader(inputStream)) }
        val fieldDefs = fieldConfigRepository.getFields()
        // Article ID is now configurable via field_definitions, no separate articleIdMode parameter
        _result.value = itemRepository.bulkImport(rows, fieldDefs)
    }

    fun consumeResult() { _result.value = null }
}
