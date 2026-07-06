package com.gigakin.stockbuddy

import android.app.Application
import com.gigakin.stockbuddy.data.db.AppDatabase
import com.gigakin.stockbuddy.data.prefs.AppPrefs
import com.gigakin.stockbuddy.data.repo.*
import com.gigakin.stockbuddy.hardware.ScannerManager
import com.gigakin.stockbuddy.hardware.ScannerManagerProvider

/**
 * NFR-27: layered architecture composition root. Manual service-locator DI rather than
 * Hilt/Dagger — keeps the dependency graph simple and explicit for an MVP-scope app; swap
 * for a DI framework later if the codebase grows past what's comfortable to wire by hand.
 */
class StockBuddyApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var prefs: AppPrefs
        private set
    lateinit var scannerManager: ScannerManager
        private set

    lateinit var categoryRepository: CategoryRepository
        private set
    lateinit var fieldConfigRepository: FieldConfigRepository
        private set
    lateinit var productRepository: ProductRepository
        private set
    lateinit var itemRepository: ItemRepository
        private set
    lateinit var inventoryRepository: InventoryRepository
        private set
    lateinit var exportRepository: ExportRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        prefs = AppPrefs(this)
        scannerManager = ScannerManagerProvider.get(this)

        categoryRepository = CategoryRepository(database.categoryDao())
        fieldConfigRepository = FieldConfigRepository(database.fieldDefinitionDao(), prefs)
        productRepository = ProductRepository(database.productMasterDao())
        itemRepository = ItemRepository(database.linkedItemDao(), database.fieldDefinitionDao(), productRepository, database.categoryDao())
        inventoryRepository = InventoryRepository(database.inventorySessionDao(), database.sessionTagDao(), database.linkedItemDao(), database.sessionResultItemDao())
        exportRepository = ExportRepository(this)
    }
}
