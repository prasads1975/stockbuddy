package com.gigakin.stockbuddy.ui.linking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.repo.ExportRepository
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
    private val fieldConfigRepository: FieldConfigRepository,
    private val exportRepository: ExportRepository
) : ViewModel() {

    sealed class TemplateDownloadResult {
        data class Success(val location: String) : TemplateDownloadResult()
        object Failure : TemplateDownloadResult()
    }

    private val _result = MutableLiveData<ItemRepository.BulkImportResult?>()
    val result: LiveData<ItemRepository.BulkImportResult?> get() = _result

    private val _templateResult = MutableLiveData<TemplateDownloadResult?>()
    val templateResult: LiveData<TemplateDownloadResult?> get() = _templateResult

    /** Domain-specific fields for the "Expected CSV Schema" preview (showOnCsv-filtered by the view). */
    val fieldDefinitions: LiveData<List<FieldDefinitionEntity>> = fieldConfigRepository.observeFields()

    /** FR-16: downloads a header+example CSV matching the Expected CSV Schema, to public Downloads. */
    fun downloadTemplate() = viewModelScope.launch {
        val fieldDefs = withContext(Dispatchers.IO) { fieldConfigRepository.getFields() }
        val rows = exportRepository.buildImportTemplateCsv(fieldDefs)
        val fileName = exportRepository.buildTemplateFileName()
        val location = withContext(Dispatchers.IO) { exportRepository.saveCsvToDownloads(fileName, rows) }
        _templateResult.value = if (location != null) TemplateDownloadResult.Success(location) else TemplateDownloadResult.Failure
    }

    fun consumeTemplateResult() { _templateResult.value = null }

    fun importCsv(inputStream: java.io.InputStream) = viewModelScope.launch {
        try {
            android.util.Log.d("BulkLinking", "importCsv called")
            val rows = withContext(Dispatchers.IO) {
                android.util.Log.d("BulkLinking", "Reading CSV...")
                inputStream.use { stream ->
                    CsvUtils.read(InputStreamReader(stream))
                }
            }
            android.util.Log.d("BulkLinking", "Read ${rows.size} rows from CSV")
            val fieldDefs = fieldConfigRepository.getFields()
            android.util.Log.d("BulkLinking", "Got ${fieldDefs.size} field definitions")
            val result = itemRepository.bulkImport(rows, fieldDefs)
            android.util.Log.d("BulkLinking", "Import complete: ${result.inserted} inserted, ${result.skipped} skipped, ${result.rejected} rejected")
            _result.value = result
        } catch (e: Exception) {
            android.util.Log.e("BulkLinking", "Error importing CSV", e)
            _result.value = null
        }
    }

    fun consumeResult() { _result.value = null }
}
