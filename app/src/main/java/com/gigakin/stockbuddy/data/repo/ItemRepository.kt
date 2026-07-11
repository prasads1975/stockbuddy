package com.gigakin.stockbuddy.data.repo

import android.util.Log
import com.gigakin.stockbuddy.data.db.dao.CategoryDao
import com.gigakin.stockbuddy.data.db.dao.FieldDefinitionDao
import com.gigakin.stockbuddy.data.db.dao.LinkedItemDao
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.util.DemoLimits
import com.gigakin.stockbuddy.util.JsonAttributes

/**
 * Individual + Bulk Linking core logic (FR-05-19) and the demo item cap.
 * v2 (§4.0.6): normalized units. Product is written FIRST (FK parent) and is
 * insert-or-reject-on-name-mismatch — never overwritten. Category is an FK (resolved by name,
 * no auto-create). The unit row (linked_items) holds no product data.
 */
class ItemRepository(
    private val linkedItemDao: LinkedItemDao,
    private val fieldDefDao: FieldDefinitionDao,
    private val productRepository: ProductRepository,
    private val categoryDao: CategoryDao
) {
    private companion object { const val TAG = "ItemRepository" }

    suspend fun getByRfid(rfid: String): LinkedItemEntity? = linkedItemDao.getByRfid(rfid)
    suspend fun countLinkedForBarcode(barcode: String): Int = linkedItemDao.countByBarcode(barcode)

    sealed class SaveResult {
        object Success : SaveResult()
        data class ValidationError(val fieldErrors: Map<String, String>) : SaveResult()
        object DuplicateRfid : SaveResult()
        object DemoLimitReached : SaveResult()
        data class DatabaseError(val message: String) : SaveResult()
    }

    /**
     * FR-08/09/09a: save-time validation for Individual Linking.
     * Category must already exist (FK, no auto-create). Product written first; name conflict rejects.
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

        // Category must exist (FK) — no auto-create (§4.0.6).
        val categoryId = categoryDao.findIdByName(category)
            ?: return SaveResult.ValidationError(mapOf("category" to "Category doesn't exist — add it in Settings first"))

        // FR-08: RFID uniqueness.
        if (linkedItemDao.getByRfid(rfidTagId) != null) return SaveResult.DuplicateRfid

        // Demo item cap.
        if (linkedItemDao.count() >= DemoLimits.MAX_ITEMS) return SaveResult.DemoLimitReached

        return try {
            val attributesJson = JsonAttributes.fromMap(attributes)

            // Product FIRST (FK parent): insert if new, reject on name conflict, else leave as-is.
            val existingProduct = productRepository.getByBarcode(barcode)
            if (existingProduct == null) {
                productRepository.insert(barcode, productName, categoryId, attributesJson)
            } else if (existingProduct.productName != productName) {
                return SaveResult.ValidationError(
                    mapOf("barcode" to "Barcode already exists as '${existingProduct.productName}'")
                )
            }

            // Normalized unit row — no product fields.
            linkedItemDao.insert(LinkedItemEntity(rfidTagId = rfidTagId, barcode = barcode))
            SaveResult.Success
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            Log.e(TAG, "Constraint violation saving linked item", e)
            SaveResult.DatabaseError("Unable to save item. Please check all fields are filled correctly.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving linked item", e)
            SaveResult.DatabaseError("Unable to save item. Please try again.")
        }
    }

    /** inserted = new units; skipped = RFID already existed (append-only); rejected = errors. */
    data class BulkImportResult(val inserted: Int, val skipped: Int, val rejected: Int, val reasons: List<String>)

    /**
     * FR-15-19: Bulk Linking CSV import (append-only, §4.0.6). Columns: Name, Barcode, Category, RFID
     * [, domain field columns...]. Category must already exist; product insert-or-reject-on-name.
     */
    suspend fun bulkImport(
        rows: List<Array<String>>,
        fieldDefs: List<FieldDefinitionEntity>
    ): BulkImportResult {
        if (rows.isEmpty()) return BulkImportResult(0, 0, 0, emptyList())

        val header = rows.first().map { it.trim() }
        val dataRows = rows.drop(1)
        fun colIndex(name: String) = header.indexOfFirst { it.equals(name, ignoreCase = true) }
        // Column names match the on-screen "Expected CSV Schema" table / downloadable template.
        val iName = colIndex("Product Name"); val iBarcode = colIndex("Barcode")
        val iCategory = colIndex("Category"); val iRfid = colIndex("RFID ID")

        var inserted = 0; var skipped = 0; var rejected = 0
        val reasons = mutableListOf<String>()
        val seenRfids = mutableSetOf<String>()

        for ((rowIdx, row) in dataRows.withIndex()) {
            val lineNo = rowIdx + 2
            fun get(i: Int) = if (i in row.indices) row[i].trim() else ""
            val name = get(iName); val barcode = get(iBarcode)
            val category = get(iCategory); val rfid = get(iRfid)

            if (name.isBlank() || barcode.isBlank() || category.isBlank() || rfid.isBlank()) {
                rejected++; reasons.add("Row $lineNo: missing mandatory field"); continue
            }
            if (rfid in seenRfids) {
                rejected++; reasons.add("Row $lineNo: duplicate RFID within file"); continue
            }
            seenRfids.add(rfid)
            if (linkedItemDao.count() >= DemoLimits.MAX_ITEMS) {
                rejected++; reasons.add("Row $lineNo: demo item limit reached"); continue
            }

            try {
                val categoryId = categoryDao.findIdByName(category)
                if (categoryId == null) {
                    rejected++
                    reasons.add("Row $lineNo: Category '$category' does not exist. Add it in Settings → Categories first.")
                    continue
                }

                // Product: insert if new, reject on name conflict, else leave as-is.
                val existingProduct = productRepository.getByBarcode(barcode)
                if (existingProduct == null) {
                    val attrs = fieldDefs.associate { fd ->
                        val ci = colIndex(fd.label)
                        fd.key to (if (ci >= 0) get(ci) else "")
                    }
                    productRepository.insert(barcode, name, categoryId, JsonAttributes.fromMap(attrs))
                } else if (existingProduct.productName != name) {
                    rejected++
                    reasons.add("Row $lineNo: Barcode '$barcode' already exists as '${existingProduct.productName}' (uploaded: '$name')")
                    continue
                }

                // Unit: insert if RFID new, else skip (append-only).
                if (linkedItemDao.getByRfid(rfid) == null) {
                    linkedItemDao.insert(LinkedItemEntity(rfidTagId = rfid, barcode = barcode))
                    inserted++
                } else {
                    skipped++
                }
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                rejected++
                reasons.add("Row $lineNo: Database error - ${e.message}")
                Log.e(TAG, "Row $lineNo constraint violation", e)
            } catch (e: Exception) {
                rejected++
                reasons.add("Row $lineNo: Unexpected error - ${e.message ?: e.javaClass.simpleName}")
                Log.e(TAG, "Row $lineNo error", e)
            }
        }
        Log.d(TAG, "bulkImport: inserted=$inserted, skipped=$skipped, rejected=$rejected")
        return BulkImportResult(inserted, skipped, rejected, reasons)
    }
}
