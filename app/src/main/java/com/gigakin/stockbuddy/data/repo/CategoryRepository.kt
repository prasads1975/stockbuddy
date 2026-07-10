package com.gigakin.stockbuddy.data.repo

import androidx.lifecycle.LiveData
import com.gigakin.stockbuddy.data.db.dao.CategoryDao
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.util.DemoLimits
import com.gigakin.stockbuddy.util.LimitCheck

/** FR-12, FR-72/73: Category master list + demo cap (Section 4, MVP Scope doc). */
class CategoryRepository(private val dao: CategoryDao) {

    fun observeAll(): LiveData<List<CategoryEntity>> = dao.observeAll()
    suspend fun getAll(): List<CategoryEntity> = dao.getAll()

    suspend fun add(name: String): LimitCheck {
        if (dao.countByName(name) > 0) return LimitCheck.Ok // FR-73: non-duplicate, silently fine if exists
        if (dao.count() >= DemoLimits.MAX_CATEGORIES) {
            return LimitCheck.Exceeded(
                "Demo limit reached (${DemoLimits.MAX_CATEGORIES} categories). Edit or delete an existing category to add a new one."
            )
        }
        try {
            android.util.Log.d("CategoryRepository", "Inserting category: $name")
            dao.insert(CategoryEntity(name = name))
            android.util.Log.d("CategoryRepository", "Category inserted successfully: $name")
            return LimitCheck.Ok
        } catch (e: Exception) {
            android.util.Log.e("CategoryRepository", "Failed to insert category: $name", e)
            return LimitCheck.Exceeded("Failed to add category: ${e.message}")
        }
    }

    suspend fun update(category: CategoryEntity) = dao.update(category)
    suspend fun delete(category: CategoryEntity) = dao.delete(category)
    suspend fun exists(name: String): Boolean = dao.countByName(name) > 0

    /**
     * True if [name] (case-insensitive) is already used by a category other than [excludeId].
     * Add → pass excludeId = null; Edit → pass the category being renamed so its own name is allowed.
     * Guards both the silent add-duplicate no-op and the rename-to-duplicate UNIQUE crash.
     */
    suspend fun isNameTaken(name: String, excludeId: Long?): Boolean {
        val existingId = dao.findIdByName(name) ?: return false
        return excludeId == null || existingId != excludeId
    }
}
