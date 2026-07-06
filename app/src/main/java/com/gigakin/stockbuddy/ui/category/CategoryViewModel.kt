package com.gigakin.stockbuddy.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.data.repo.ProductRepository
import com.gigakin.stockbuddy.util.LimitCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** FR-72/73 + Section 4 demo cap (10 categories). */
class CategoryViewModel(
    private val repository: CategoryRepository,
    private val productRepository: ProductRepository
) : ViewModel() {
    val categories: LiveData<List<CategoryEntity>> = repository.observeAll()

    private val _addResult = MutableLiveData<LimitCheck?>()
    val addResult: LiveData<LimitCheck?> get() = _addResult

    fun add(name: String) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) { repository.add(name) }
        _addResult.value = result
    }
    fun update(category: CategoryEntity) = viewModelScope.launch(Dispatchers.IO) { repository.update(category) }

    /** §4.0.6: deleting a category cascades to its products and their linked units (FK CASCADE). */
    fun delete(category: CategoryEntity) = viewModelScope.launch(Dispatchers.IO) { repository.delete(category) }

    /** Count of products that would be cascade-deleted with this category (for the warning dialog). */
    fun productCount(categoryId: Long, cb: (Int) -> Unit) = viewModelScope.launch {
        cb(productRepository.countByCategory(categoryId))
    }

    fun consumeAddResult() { _addResult.value = null }
}
