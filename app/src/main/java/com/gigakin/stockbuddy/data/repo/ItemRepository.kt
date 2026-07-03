package com.gigakin.stockbuddy.data.repo

import android.util.Log
import androidx.lifecycle.LiveData
import com.gigakin.stockbuddy.data.db.dao.FieldDefinitionDao
import com.gigakin.stockbuddy.data.db.dao.LinkedItemDao
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.util.DemoLimits

/**
 * Individual + Bulk Linking core logic (FR-05-10, FR-09a-c, FR-15-19), Assets (FR-61-67),
 * and the demo item cap (Section 4, MVP Scope doc).
 * Barcode is the primary business identifier and links to product_master.
 */
class ItemRepository(
    private val linkedItemDao: LinkedItemDao,
    private val fieldDefDao: FieldDefinitionDao,
    private val productRepository: ProductRepository
) {
    fun observeAll(): LiveData<List<LinkedItemEntity>> = linkedItemDao.observeAll()
    fun search(query: String, category: String?): LiveData<List<LinkedItemEntity>> =
        linkedItemDao.search(query, category)
    suspend fun getAll(): List<LinkedItemEntity> = linkedItemDao.getAll()
    suspend fun getByRfid(rfid: String): LinkedItemEntity? = linkedItemDao.getByRfid(rfid)
    suspend fun delete(item: LinkedItemEntity) = linkedItemDao.delete(item)
    suspend fun update(item: LinkedItemEntity) = linkedItemDao.update(item)

    sealed class SaveResult {
        object Success : SaveResult()
        data class ValidationError(val fieldErrors: Map<String, String>) : SaveResult()
        object DuplicateRfid : SaveResult()
        object DemoLimitReached : SaveResult()
        data class DatabaseError(val message: String) : SaveResult()
    }

    /**
     * FR-08/09/09a/09b/09c: full save-time validation for Individual Linking.
     * Fixed mandatory fields: productName, barcode, category, rfidTagId.
     * Configured-mandatory domain-specific fields from field_definitions.
     * On success, auto-upserts the Product Master (FR-21).
     */
    suspend fun saveLinkedItem(
        productName: String,
        barcode: String,
        category: String,
        rfidTagId: String,
        attributes: Map<String, String>
    ): SaveResult {
        val errors = mutableMapOf<String, String>()
        if (productName.isBlank()) errors["name"] = "Required"
        if (barcode.isBlank()) errors["barcode"] = "Required"
        if (category.isBlank()) errors["category"] = "Required"
        if (rfidTagId.isBlank()) errors["rfid"] = "Required"

        // FR-09a: configured-mandatory domain-specific fields.
        val fieldDefs = fieldDefDao.getAll()
        fieldDefs.filter { it.mandatory && it.showOnLinking }.forEach { f ->
            if (attributes[f.key].isNullOrBlank()) errors["attr_${f.key}"] = "Required"
        }

        if (errors.isNotEmpty()) return SaveResult.ValidationError(errors)

        // FR-08: RFID uniqueness, DB-enforced (NFR-18) — checked here too for a clean error.
        if (linkedItemDao.getByRfid(rfidTagId) != null) return SaveResult.DuplicateRfid

        // Demo item cap (Section 4, MVP Scope doc) — applies to manual Linking saves.
        if (linkedItemDao.count() >= DemoLimits.MAX_ITEMS) return SaveResult.DemoLimitReached

        return try {
            // FR-21: auto-upsert Product Master FIRST (before item insert) to satisfy FK constraint.
            productRepository.upsertFromLinkedItem(
                barcode, productName, category,
                com.gigakin.stockbuddy.util.JsonAttributes.fromMap(attributes)
            )

            linkedItemDao.insert(
                LinkedItemEntity(
                    rfidTagId = rfidTagId,
                    productName = productName,
                    barcode = barcode,
                    category = category,
                    attributesJson = com.gigakin.stockbuddy.util.JsonAttributes.fromMap(attributes)
                )
            )

            SaveResult.Success
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            Log.e("ItemRepository", "Database constraint violation while saving linked item", e)
            val userMessage = when {
                e.message?.contains("FOREIGN KEY", ignoreCase = true) == true ->
                    "The category doesn't exist. Please create it in Settings → Categories first."
                e.message?.contains("UNIQUE", ignoreCase = true) == true ->
                    "This RFID tag is already linked to another item."
                else -> "Unable to save item. Please check all fields are filled correctly."
            }
            SaveResult.DatabaseError(userMessage)
        } catch (e: Exception) {
            Log.e("ItemRepository", "Unexpected error while saving linked item", e)
            SaveResult.DatabaseError("Unable to save item. Please try again.")
        }
    }

    data class BulkImportResult(val inserted: Int, val updated: Int, val rejected: Int, val reasons: List<String>)

    /**
     * FR-15-19: Bulk Linking CSV upsert. Expected columns (in order): Name, Barcode,
     * Category, RFID[, domain-specific field columns...].
     * Mandatory columns: Name, Barcode, Category, RFID (FR-16).
     * Demo cap applies here too (Section 4).
     */
    suspend fun bulkImport(
        rows: List<Array<String>>,
        fieldDefs: List<FieldDefinitionEntity>
    ): BulkImportResult {
        if (rows.isEmpty()) return BulkImportResult(0, 0, 0, emptyList())
        val header = rows.first().map { it.trim() }
        val dataRows = rows.drop(1)

        fun colIndex(name: String) = header.indexOfFirst { it.equals(name, ignoreCase = true) }
        val iName = colIndex("Name")
        val iBarcode = colIndex("Barcode")
        val iCategory = colIndex("Category")
        val iRfid = colIndex("RFID")

        var inserted = 0; var updated = 0; var rejected = 0
        val reasons = mutableListOf<String>()
        val seenRfids = mutableSetOf<String>()

        for ((rowIdx, row) in dataRows.withIndex()) {
            fun get(i: Int) = if (i in row.indices) row[i].trim() else ""
            val name = get(iName)
            val barcode = get(iBarcode)
            val category = get(iCategory)
            val rfid = get(iRfid)

            if (name.isBlank() || barcode.isBlank() || category.isBlank() || rfid.isBlank()) {
                rejected++; reasons.add("Row ${rowIdx + 2}: missing mandatory field"); continue
            }
            if (rfid in seenRfids) {
                rejected++; reasons.add("Row ${rowIdx + 2}: duplicate RFID within file"); continue
            }
            seenRfids.add(rfid)

            if (linkedItemDao.count() >= DemoLimits.MAX_ITEMS) {
                rejected++; reasons.add("Row ${rowIdx + 2}: demo item limit reached"); continue
            }

            val existing = linkedItemDao.getByRfid(rfid)
            // Build attributes map from all configured field_definitions
            val attrs = fieldDefs.associate { fd ->
                val ci = colIndex(fd.label)
                fd.key to (if (ci >= 0) get(ci) else "")
            }

            val entity = LinkedItemEntity(
                rfidTagId = rfid,
                productName = name,
                barcode = barcode,
                category = category,
                attributesJson = com.gigakin.stockbuddy.util.JsonAttributes.fromMap(attrs)
            )
            if (existing == null) { linkedItemDao.insert(entity); inserted++ }
            else { linkedItemDao.update(entity); updated++ }

            productRepository.upsertFromLinkedItem(
                barcode, name, category,
                com.gigakin.stockbuddy.util.JsonAttributes.fromMap(attrs)
            )
        }
        return BulkImportResult(inserted, updated, rejected, reasons)
    }
}
