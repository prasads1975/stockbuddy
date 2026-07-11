package com.gigakin.stockbuddy.ui.assets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.db.dao.ProductSummary
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.data.repo.CategoryRepository
import com.gigakin.stockbuddy.data.repo.ItemRepository
import com.gigakin.stockbuddy.data.repo.ProductRepository
import kotlinx.coroutines.launch

/**
 * FR-61-67: Assets is now PRODUCT-level (v2, §4.0.6) — one row per product with a linked-unit
 * count. Search (FR-63), category filter (FR-67), live count (FR-64). Edit/delete act on the product;
 * delete cascades to the product's linked units. Also exposes per-tag operations for the
 * expandable "RFID Tags (N)" row on each card (delete a single tag, reassign it to another product).
 */
class AssetsViewModel(
    private val productRepository: ProductRepository,
    private val itemRepository: ItemRepository,
    val categoryRepository: CategoryRepository
) : ViewModel() {

    private val query = MutableLiveData("")
    private val categoryId = MutableLiveData<Long?>(null)

    val items: LiveData<List<ProductSummary>> =
        query.switchMap { q -> productRepository.search(q, categoryId.value) }

    /** The query that produced the current `items` emission — read alongside it to drive RFID-match auto-expand. */
    val currentQuery: String get() = query.value.orEmpty()

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

    /** Individual tags behind a card's "RFID Tags (N)" expand row. */
    fun loadTags(barcode: String, onLoaded: (List<LinkedItemEntity>) -> Unit) {
        viewModelScope.launch { onLoaded(itemRepository.getTagsForBarcode(barcode)) }
    }

    fun deleteTag(rfid: String, onDone: () -> Unit) {
        viewModelScope.launch { itemRepository.deleteTag(rfid); onDone() }
    }

    fun reassignTag(rfid: String, newBarcode: String, onDone: (ItemRepository.ReassignResult) -> Unit) {
        viewModelScope.launch { onDone(itemRepository.reassignTag(rfid, newBarcode)) }
    }
}
