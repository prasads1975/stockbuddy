package com.gigakin.stockbuddy.ui.assets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.data.repo.ItemRepository

/** FR-61-67: Assets flat list, search (FR-63), category filter (FR-67), live count (FR-64). */
class AssetsViewModel(
    private val itemRepository: ItemRepository,
    val categoryRepository: CategoryRepository
) : ViewModel() {

    private val query = MutableLiveData("")
    private val category = MutableLiveData<String?>(null)

    val items: LiveData<List<com.gigakin.stockbuddy.data.db.entity.ItemEntity>> =
        query.switchMap { q -> itemRepository.search(q, category.value) }

    fun setQuery(q: String) { query.value = q }
    fun setCategory(c: String?) { category.value = c; query.value = query.value } // re-trigger switchMap
}
