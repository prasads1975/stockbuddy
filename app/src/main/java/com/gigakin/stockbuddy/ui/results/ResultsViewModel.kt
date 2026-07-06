package com.gigakin.stockbuddy.ui.results

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.data.repo.InventoryRepository
import kotlinx.coroutines.launch

/** FR-40-47: Available/Missing/Excess computation, category filter carry-over (FR-46). */
class ResultsViewModel(
    private val inventoryRepository: InventoryRepository,
    val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _allResults = MutableLiveData<List<InventoryRepository.ResultItem>>()
    private val _categoryFilter = MutableLiveData<String?>(null)
    val categoryFilter: LiveData<String?> get() = _categoryFilter

    val filteredResults = MutableLiveData<List<InventoryRepository.ResultItem>>()

    fun load(sessionId: String) = viewModelScope.launch {
        val results = inventoryRepository.computeResults(sessionId, null)
        _allResults.value = results
        applyFilter()
    }

    /** FR-46: re-apply filter to the already-loaded raw set, no re-scan needed. */
    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
        applyFilter()
    }

    private fun applyFilter() {
        val filter = _categoryFilter.value
        val all = _allResults.value ?: emptyList()
        filteredResults.value = if (filter == null) all else all.filter { it.category == filter || it.status == InventoryRepository.Status.EXCESS }
    }

    fun currentResults(): List<InventoryRepository.ResultItem> = filteredResults.value ?: emptyList()
}
