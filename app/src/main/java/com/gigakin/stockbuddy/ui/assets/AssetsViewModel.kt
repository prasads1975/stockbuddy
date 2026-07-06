package com.gigakin.stockbuddy.ui.assets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.db.dao.ProductSummary
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.data.repo.ProductRepository
import kotlinx.coroutines.launch

/**
 * FR-61-67: Assets is now PRODUCT-level (v2, §4.0.6) — one row per product with a linked-unit
 * count. Search (FR-63), category filter (FR-67), live count (FR-64). Edit/delete act on the product;
 * delete cascades to the product's linked units.
 */
class AssetsViewModel(
    private val productRepository: ProductRepository,
    val categoryRepository: CategoryRepository
) : ViewModel() {

    private val query = MutableLiveData("")
    private val categoryId = MutableLiveData<Long?>(null)

    val items: LiveData<List<ProductSummary>> =
        query.switchMap { q -> productRepository.search(q, categoryId.value) }

    fun setQuery(q: String) { query.value = q }
    fun setCategory(id: Long?) { categoryId.value = id; query.value = query.value }

    fun deleteProduct(barcode: String) {
        viewModelScope.launch {
            productRepository.getByBarcode(barcode)?.let { productRepository.delete(it) }
        }
    }

    /** Edit product master; category resolved by name. Returns via callback (true = saved). */
    fun updateProduct(barcode: String, name: String, categoryName: String, attributesJson: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val categoryId = categoryRepository.getAll().firstOrNull { it.name.equals(categoryName, true) }?.id
            val existing = productRepository.getByBarcode(barcode)
            if (categoryId == null || existing == null) { onDone(false); return@launch }
            productRepository.update(existing.copy(productName = name, categoryId = categoryId, attributesJson = attributesJson))
            onDone(true)
        }
    }
}
