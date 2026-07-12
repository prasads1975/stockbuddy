package com.gigakin.stockbuddy.data.db.dao

import androidx.room.*
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity

/**
 * A master unit joined with its product details — used to build the results snapshot at STOP
 * (System Design §4.0.5). The join happens once per session, never on the scan hot path.
 */
data class MasterUnitRow(
    @ColumnInfo(name = "rfid_tag_id") val rfidTagId: String,
    val barcode: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "category_name") val category: String,
    @ColumnInfo(name = "attributes") val attributesJson: String
)

@Dao
interface LinkedItemDao {
    @Query("SELECT * FROM linked_items")
    suspend fun getAll(): List<LinkedItemEntity>

    /** In-memory master RFID set for live scan counts (§4.0.4) — ids only, kept lean. */
    @Query("SELECT rfid_tag_id FROM linked_items")
    suspend fun getAllRfids(): List<String>

    /** Category-scoped master RFID set, for live Available/Missing when a scan session is scoped to one category (FR-35). */
    @Query("""
        SELECT l.rfid_tag_id FROM linked_items l
        JOIN product_master p ON p.barcode = l.barcode
        JOIN categories c ON c.id = p.category_id
        WHERE c.name = :categoryName
    """)
    suspend fun getRfidsByCategory(categoryName: String): List<String>

    @Query("SELECT * FROM linked_items WHERE rfid_tag_id = :rfid LIMIT 1")
    suspend fun getByRfid(rfid: String): LinkedItemEntity?

    @Query("SELECT COUNT(*) FROM linked_items")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM linked_items WHERE barcode = :barcode")
    suspend fun countByBarcode(barcode: String): Int

    /** Individual tags for a product, for the Assets screen's expandable per-tag list. */
    @Query("SELECT * FROM linked_items WHERE barcode = :barcode ORDER BY linkedAt ASC")
    suspend fun getByBarcode(barcode: String): List<LinkedItemEntity>

    @Query("DELETE FROM linked_items WHERE rfid_tag_id = :rfid")
    suspend fun deleteByRfid(rfid: String)

    /** Reassign a tag to a different product. RFID (PK) is unchanged; only the barcode FK moves. */
    @Query("UPDATE linked_items SET barcode = :newBarcode WHERE rfid_tag_id = :rfid")
    suspend fun reassignBarcode(rfid: String, newBarcode: String)

    /** Full master with product details (join), for one-time snapshot computation at STOP. */
    @Query("""
        SELECT l.rfid_tag_id, l.barcode, p.product_name, c.name AS category_name, p.attributes
        FROM linked_items l
        JOIN product_master p ON p.barcode = l.barcode
        JOIN categories c ON c.id = p.category_id
    """)
    suspend fun getMasterWithDetails(): List<MasterUnitRow>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: LinkedItemEntity)

    @Delete
    suspend fun delete(item: LinkedItemEntity)
}
