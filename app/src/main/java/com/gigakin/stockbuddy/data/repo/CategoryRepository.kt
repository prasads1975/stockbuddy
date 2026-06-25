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
        dao.insert(CategoryEntity(name = name))
        return LimitCheck.Ok
    }

    suspend fun update(category: CategoryEntity) = dao.update(category)
    suspend fun delete(category: CategoryEntity) = dao.delete(category)
    suspend fun exists(name: String): Boolean = dao.countByName(name) > 0
}
