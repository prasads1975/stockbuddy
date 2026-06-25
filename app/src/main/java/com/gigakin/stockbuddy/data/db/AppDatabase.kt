package com.gigakin.stockbuddy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gigakin.stockbuddy.data.db.dao.*
import com.gigakin.stockbuddy.data.db.entity.*

/**
 * NFR-15: local-only SQLite via Room. NFR-18: RFID uniqueness enforced at the DB layer
 * (ItemEntity.rfidTagId is the @PrimaryKey). Barcode/RFID columns are indexed by virtue
 * of being primary keys — see Section 1.11 (Architecture & Scalability Principles) in the
 * MVP scope doc for why this matters even at demo data volumes.
 */
@Database(
    entities = [
        CategoryEntity::class,
        FieldDefinitionEntity::class,
        ProductEntity::class,
        ItemEntity::class,
        InventorySessionEntity::class,
        ScannedTagEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun fieldDefinitionDao(): FieldDefinitionDao
    abstract fun productDao(): ProductDao
    abstract fun itemDao(): ItemDao
    abstract fun inventorySessionDao(): InventorySessionDao
    abstract fun scannedTagDao(): ScannedTagDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stockbuddy.db"
                ).build().also { INSTANCE = it }
            }
    }
}
