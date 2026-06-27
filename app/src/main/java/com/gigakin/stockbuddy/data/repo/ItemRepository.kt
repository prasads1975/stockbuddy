package com.gigakin.stockbuddy.data.repo

import androidx.lifecycle.LiveData
import com.gigakin.stockbuddy.data.db.dao.FieldDefinitionDao
import com.gigakin.stockbuddy.data.db.dao.ItemDao
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.db.entity.ItemEntity
import com.gigakin.stockbuddy.util.ArticleIdMode
import com.gigakin.stockbuddy.util.DemoLimits

/**
 * Individual + Bulk Linking core logic (FR-05-10, FR-09a-c, FR-15-19), Assets (FR-61-67),
 * and the demo item cap (Section 4, MVP Scope doc).
 */
class ItemRepository(
    private val itemDao: ItemDao,
    private val fieldDefDao: FieldDefinitionDao,
    private val productRepository: ProductRepository
) {
    fun observeAll(): LiveData<List<ItemEntity>> = itemDao.observeAll()
    fun search(query: String, category: String?): LiveData<List<ItemEntity>> =
        itemDao.search(query, category)
    suspend fun getAll(): List<ItemEntity> = itemDao.getAll()
    suspend fun delete(item: ItemEntity) = itemDao.delete(item)

    sealed class SaveResult {
        object Success : SaveResult()
        data class ValidationError(val fieldErrors: Map<String, String>) : SaveResult()
        object DuplicateRfid : SaveResult()
        object DemoLimitReached : SaveResult()
    }

    /**
     * FR-08/09/09a/09b/09c: full save-time validation for Individual Linking, both fixed
     * mandatory fields and configured-mandatory domain-specific fields (including Article ID).
     * Article ID is now configurable via field_definitions. On success, auto-upserts the
     * Product Master (FR-21).
     */
    suspend fun saveLinkedItem(
        name: String,
        barcode: String,
        categoryName: String,
        rfidTagId: String,
        attributes: Map<String, String>
    ): SaveResult {
        val errors = mutableMapOf<String, String>()
        if (name.isBlank()) errors["name"] = "Required"
        if (barcode.isBlank()) errors["barcode"] = "Required"
        if (categoryName.isBlank()) errors["category"] = "Required"
        if (rfidTagId.isBlank()) errors["rfid"] = "Required"

        // FR-09a: configured-mandatory domain-specific fields (including articleId if configured).
        val fieldDefs = fieldDefDao.getAll()
        fieldDefs.filter { it.mandatory && it.showOnLinking }.forEach { f ->
            if (attributes[f.key].isNullOrBlank()) errors[f.key] = "Required"
        }

        if (errors.isNotEmpty()) return SaveResult.ValidationError(errors)

        // FR-08: RFID uniqueness, DB-enforced (NFR-18) — checked here too for a clean error.
        if (itemDao.getByRfid(rfidTagId) != null) return SaveResult.DuplicateRfid

        // Demo item cap (Section 4, MVP Scope doc) — applies to manual Linking saves.
        if (itemDao.count() >= DemoLimits.MAX_ITEMS) return SaveResult.DemoLimitReached

        itemDao.insert(
            ItemEntity(
                rfidTagId = rfidTagId,
                barcode = barcode,
                name = name,
                categoryName = categoryName,
                attributesJson = com.gigakin.stockbuddy.util.JsonAttributes.fromMap(attributes)
            )
        )

        // FR-21: auto-upsert Product Master, matched by Barcode.
        productRepository.upsertFromLinkedItem(
            barcode, name, categoryName,
            com.gigakin.stockbuddy.util.JsonAttributes.fromMap(attributes)
        )

        return SaveResult.Success
    }

    data class BulkImportResult(val inserted: Int, val updated: Int, val rejected: Int, val reasons: List<String>)

    /**
     * FR-15-19: Bulk Linking CSV upsert. Expected columns (in order): Name, Barcode, Category,
     * RFID[, ArticleId if enabled][, domain-specific field columns...].
     * Mandatory columns: Name, Barcode, RFID (FR-16). Demo cap applies here too (Section 4).
     */
    suspend fun bulkImport(
        rows: List<Array<String>>,
        fieldDefs: List<FieldDefinitionEntity>
    ): BulkImportResult {
        if (rows.isEmpty()) return BulkImportResult(0, 0, 0, emptyList())
        val header = rows.first().map { it.trim() }
        val dataRows = rows.drop(1)

        fun colIndex(name: String) = header.indexOfFirst { it.equals(name, ignoreCase = true) }
        val iName = colIndex("Name"); val iBarcode = colIndex("Barcode")
        val iCategory = colIndex("Category"); val iRfid = colIndex("RFID")

        var inserted = 0; var updated = 0; var rejected = 0
        val reasons = mutableListOf<String>()
        val seenRfids = mutableSetOf<String>()

        for ((rowIdx, row) in dataRows.withIndex()) {
            fun get(i: Int) = if (i in row.indices) row[i].trim() else ""
            val name = get(iName); val barcode = get(iBarcode)
            val category = get(iCategory); val rfid = get(iRfid)

            if (name.isBlank() || barcode.isBlank() || rfid.isBlank()) {
                rejected++; reasons.add("Row ${rowIdx + 2}: missing mandatory field"); continue
            }
            if (rfid in seenRfids) {
                rejected++; reasons.add("Row ${rowIdx + 2}: duplicate RFID within file"); continue
            }
            seenRfids.add(rfid)

            if (itemDao.count() >= DemoLimits.MAX_ITEMS) {
                rejected++; reasons.add("Row ${rowIdx + 2}: demo item limit reached"); continue
            }

            val existing = itemDao.getByRfid(rfid)
            // Build attributes map from all configured field_definitions
            val attrs = fieldDefs.associate { fd ->
                val ci = colIndex(fd.label)
                fd.key to (if (ci >= 0) get(ci) else "")
            }

            val entity = ItemEntity(
                rfidTagId = rfid, barcode = barcode, name = name, categoryName = category,
                attributesJson = com.gigakin.stockbuddy.util.JsonAttributes.fromMap(attrs)
            )
            if (existing == null) { itemDao.insert(entity); inserted++ }
            else { itemDao.update(entity); updated++ }

            productRepository.upsertFromLinkedItem(
                barcode, name, category,
                com.gigakin.stockbuddy.util.JsonAttributes.fromMap(attrs)
            )
        }
        return BulkImportResult(inserted, updated, rejected, reasons)
    }
}
