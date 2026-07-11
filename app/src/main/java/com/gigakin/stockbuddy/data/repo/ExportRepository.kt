package com.gigakin.stockbuddy.data.repo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.util.JsonAttributes
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
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
            val attrs = JsonAttributes.toMap(r.attributesJson)
            val row = mutableListOf(r.status.name, r.productName, r.barcode, r.category)
            // Domain-specific fields
            fieldDefs.forEach { row.add(attrs[it.key] ?: "") }
            row.add(r.rfidTagId)
            rows.add(row.toTypedArray())
        }
        return rows
    }

    /** FR-16: filename for the downloadable Bulk Linking import template. */
    fun buildTemplateFileName(): String = "StockBuddy_Bulk_Linking_Template.csv"

    /**
     * FR-16: header + one example row for the downloadable Bulk Linking CSV template.
     * Column names match exactly what ItemRepository.bulkImport() looks for (Product Name,
     * Barcode, Category, RFID ID), plus any showOnCsv domain-specific fields by their label.
     */
    fun buildImportTemplateCsv(fieldDefs: List<FieldDefinitionEntity>): List<Array<String>> {
        val customFields = fieldDefs.filter { it.showOnCsv }

        val header = mutableListOf("Product Name", "Barcode", "Category", "RFID ID")
        customFields.forEach { header.add(it.label) }

        val example = mutableListOf("Rugged Smartphone", "88291022", "Accessory", "E28011912345678")
        customFields.forEach { example.add(templateExampleFor(it)) }

        return listOf(header.toTypedArray(), example.toTypedArray())
    }

    private fun templateExampleFor(field: FieldDefinitionEntity): String = when (field.type) {
        "NUMBER" -> "19.99"
        "DATE" -> "2026-01-15"
        "DROPDOWN" -> field.dropdownOptionsCsv?.split(",")?.map { it.trim() }?.firstOrNull { it.isNotEmpty() } ?: "Option"
        else -> "Sample text"
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
     * FR-52a: Download (save to device). Writes the CSV to the **public Downloads collection** so
     * the user can actually find it. API 29+ uses MediaStore (no storage permission needed); older
     * devices fall back to the app's exports/ dir. Returns a user-facing location string, or null on
     * failure.
     */
    fun saveCsvToDownloads(fileName: String, rows: List<Array<String>>): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            return try {
                resolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { w -> CSVWriter(w).use { it.writeAll(rows.toMutableList()) } }
                } ?: run { resolver.delete(uri, null, null); return null }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Downloads/$fileName"
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        }
        // API 28 fallback: app-private exports dir (no permission needed); surface the real path.
        return try {
            writeCsvFile(fileName, rows).absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun downloadedFilePath(file: File): String = file.absolutePath

    /** Write CSV content to a user-selected URI (via document picker / SAF). */
    fun writeCsvFileToUri(uri: Uri, rows: List<Array<String>>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    CSVWriter(writer).use { csvWriter ->
                        csvWriter.writeAll(rows.toMutableList())
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
