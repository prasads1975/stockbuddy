package com.gigakin.stockbuddy.data.repo

import android.content.Context
import androidx.core.content.FileProvider
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.util.JsonAttributes
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/** FR-49-52a: CSV generation and the Share / Download delivery channels. */
class ExportRepository(private val context: Context) {

    /** FR-51: filename from Inventory Code + timestamp. */
    fun buildFileName(inventoryCode: String): String {
        val safeCode = inventoryCode.replace(Regex("[^A-Za-z0-9]+"), "_")
        val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "${safeCode}_$ts.csv"
    }

    /**
     * FR-38/50: builds CSV rows — Status, Product Name, Barcode, Category,
     * domain-specific fields, RFID Tag ID.
     */
    fun buildCsv(
        results: List<InventoryRepository.ResultItem>,
        fieldDefs: List<FieldDefinitionEntity>
    ): List<Array<String>> {
        val header = mutableListOf("Status", "Product Name", "Barcode", "Category")
        fieldDefs.forEach { header.add(it.label) }
        header.add("RFID Tag ID")

        val rows = mutableListOf(header.toTypedArray())
        results.forEach { r ->
            val attrs = JsonAttributes.toMap(r.item.attributesJson)
            val row = mutableListOf(r.status.name, r.item.productName, r.item.barcode, r.item.category)
            // Domain-specific fields
            fieldDefs.forEach { row.add(attrs[it.key] ?: "") }
            row.add(r.item.rfidTagId)
            rows.add(row.toTypedArray())
        }
        return rows
    }

    /** Writes the CSV to the app's external files exports/ dir (shared base for FR-52 and FR-52a). */
    fun writeCsvFile(fileName: String, rows: List<Array<String>>): File {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileWriter(file).use { writer -> CSVWriter(writer).use { it.writeAll(rows.toMutableList()) } }
        return file
    }

    /** FR-52: Share sheet delivery via FileProvider. */
    fun shareIntentFor(file: File): android.content.Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * FR-52a: Download (save to device). For MVP, "location picker" is satisfied by saving to
     * the app's external-files exports/ dir (already done in writeCsvFile) and surfacing the
     * path to the user — a full SAF document-tree picker can be layered in later without
     * changing this repository's public contract.
     */
    fun downloadedFilePath(file: File): String = file.absolutePath
}
