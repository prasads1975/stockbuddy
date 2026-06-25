package com.gigakin.stockbuddy.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.util.LimitCheck
import kotlinx.coroutines.launch

/** FR-72/73 + Section 4 demo cap (10 categories). */
class CategoryViewModel(private val repository: CategoryRepository) : ViewModel() {
    val categories: LiveData<List<CategoryEntity>> = repository.observeAll()

    private val _addResult = MutableLiveData<LimitCheck?>()
    val addResult: LiveData<LimitCheck?> get() = _addResult

    fun add(name: String) = viewModelScope.launch { _addResult.value = repository.add(name) }
    fun delete(category: CategoryEntity) = viewModelScope.launch { repository.delete(category) }
    fun consumeAddResult() { _addResult.value = null }
}
