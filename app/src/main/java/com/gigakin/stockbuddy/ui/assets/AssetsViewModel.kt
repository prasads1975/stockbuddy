package com.gigakin.stockbuddy.ui.assets

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.data.repo.ItemRepository
import kotlinx.coroutines.launch

/** FR-61-67: Assets flat list, search (FR-63), category filter (FR-67), live count (FR-64). */
class AssetsViewModel(
    private val itemRepository: ItemRepository,
    val categoryRepository: CategoryRepository
) : ViewModel() {

    private val query = androidx.lifecycle.MutableLiveData("")
    private val category = androidx.lifecycle.MutableLiveData<String?>(null)

    val items: LiveData<List<LinkedItemEntity>> =
        query.switchMap { q -> itemRepository.search(q, category.value) }

    fun setQuery(q: String) { query.value = q }
    fun setCategory(c: String?) { category.value = c; query.value = query.value }

    fun deleteItem(rfidTagId: String) {
        viewModelScope.launch {
            itemRepository.deleteItem(rfidTagId)
        }
    }

    fun updateItem(item: LinkedItemEntity) {
        viewModelScope.launch {
            itemRepository.updateLinkedItem(item)
        }
    }
}
