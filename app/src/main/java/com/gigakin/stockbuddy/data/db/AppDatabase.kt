package com.gigakin.stockbuddy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gigakin.stockbuddy.data.db.dao.*
import com.gigakin.stockbuddy.data.db.entity.*

/**
 * NFR-15: local-only SQLite via Room. NFR-18: RFID uniqueness enforced at the DB layer
 * via PRIMARY KEY on linked_items.rfid_tag_id. Barcode is the PK of product_master.
 * Schema version 3: v2 data model (normalized master + snapshot) per System Design §4.0.
 * generated_reports / app_config are aspirational (not created in MVP — §4.0.1).
 */
@Database(
    entities = [
        CategoryEntity::class,
        FieldDefinitionEntity::class,
        ProductMasterEntity::class,
        LinkedItemEntity::class,
        InventorySessionEntity::class,
        SessionTagEntity::class,
        SessionResultItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun fieldDefinitionDao(): FieldDefinitionDao
    abstract fun productMasterDao(): ProductMasterDao
    abstract fun linkedItemDao(): LinkedItemDao
    abstract fun inventorySessionDao(): InventorySessionDao
    abstract fun sessionTagDao(): SessionTagDao
    abstract fun sessionResultItemDao(): SessionResultItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stockbuddy.db"
                )
                .fallbackToDestructiveMigration()  // v1→v2: MVP allows data wipe on schema change
                .build().also { INSTANCE = it }
            }
    }
}
