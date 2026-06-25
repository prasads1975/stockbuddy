package com.gigakin.stockbuddy.data.repo

import androidx.lifecycle.LiveData
import com.gigakin.stockbuddy.data.db.dao.FieldDefinitionDao
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.prefs.AppPrefs
import com.gigakin.stockbuddy.util.ArticleIdMode

/**
 * FR-77i-o / NFR-51-56: the headline dynamic-field-configuration feature, and the
 * Article ID usage toggle (NFR-56). Section 1.1 of the MVP Scope doc.
 */
class FieldConfigRepository(
    private val dao: FieldDefinitionDao,
    private val prefs: AppPrefs
) {
    fun observeFields(): LiveData<List<FieldDefinitionEntity>> = dao.observeAll()
    suspend fun getFields(): List<FieldDefinitionEntity> = dao.getAll()

    suspend fun saveFields(fields: List<FieldDefinitionEntity>) {
        dao.clearAll()
        dao.insertAll(fields.mapIndexed { i, f -> f.copy(sortOrder = i) })
    }

    var articleIdMode: ArticleIdMode
        get() = prefs.articleIdMode
        set(value) { prefs.articleIdMode = value }

    var setupCompleted: Boolean
        get() = prefs.fieldConfigCompleted
        set(value) { prefs.fieldConfigCompleted = value }

    /** FR-77l: starter templates — pre-fills the field list, fully editable afterward. */
    object Templates {
        val TOY_RETAIL = listOf(
            FieldDefinitionEntity(key = "price", label = "Price", type = "NUMBER", mandatory = false)
        )
        val JEWELLERY = listOf(
            FieldDefinitionEntity(key = "net_weight", label = "Net Weight", type = "NUMBER"),
            FieldDefinitionEntity(key = "gross_weight", label = "Gross Weight", type = "NUMBER"),
            FieldDefinitionEntity(key = "purity", label = "Purity", type = "TEXT")
        )
        val GENERIC = emptyList<FieldDefinitionEntity>()
    }
}
