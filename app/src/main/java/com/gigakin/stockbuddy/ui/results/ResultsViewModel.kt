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

    // The category selected on the Start screen (FR-35) — the session's intentional scope.
    // Immutable for the life of this load; unlike _categoryFilter (the user-changeable display
    // filter), this never changes after load() and is what applyFilter() checks against.
    // Exposed so the UI can lock the filter control entirely when the scope is a specific
    // category — offering other categories would mostly lead to a meaningless 0/0 dead end.
    private val _sessionScope = MutableLiveData<String?>(null)
    val sessionScope: LiveData<String?> get() = _sessionScope

    val filteredResults = MutableLiveData<List<InventoryRepository.ResultItem>>()

    /**
     * FR-46: always load the full raw set (all categories) so Excess is never lost, but default
     * the active filter to whatever was selected on the Start Inventory Scan screen (FR-35
     * carry-over) rather than always "All".
     */
    fun load(sessionId: String) = viewModelScope.launch {
        val session = inventoryRepository.getSession(sessionId)
        val results = inventoryRepository.computeResults(sessionId, null)
        _allResults.value = results
        _sessionScope.value = session?.categoryFilter
        _categoryFilter.value = session?.categoryFilter
        applyFilter()
    }

    /** FR-46: re-apply filter to the already-loaded raw set, no re-scan needed. */
    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
        applyFilter()
    }

    /**
     * The session was scanned with a specific category intentionally selected (FR-35) — the
     * operator was only counting that category, so Available/Missing for any OTHER category is
     * not real data and reads as 0/0. "All" is treated the same as the session's own category,
     * since that scope IS everything this session tracked. Excess is always retained regardless
     * (unscoped by definition — see computeSessionStats).
     */
    private fun applyFilter() {
        val filter = _categoryFilter.value
        val all = _allResults.value ?: emptyList()
        val scope = _sessionScope.value
        filteredResults.value = when {
            scope == null -> if (filter == null) all else all.filter { it.category == filter || it.status == InventoryRepository.Status.EXCESS }
            filter == null || filter == scope -> all.filter { it.category == scope || it.status == InventoryRepository.Status.EXCESS }
            else -> all.filter { it.status == InventoryRepository.Status.EXCESS }
        }
    }

    fun currentResults(): List<InventoryRepository.ResultItem> = filteredResults.value ?: emptyList()
}
