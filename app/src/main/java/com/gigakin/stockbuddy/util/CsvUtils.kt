package com.gigakin.stockbuddy.util

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.Reader
import java.io.Writer

/** Default CSV library decision: OpenCSV (Section 7, MVP Scope doc) — pure Java, AOSP-safe. */
object CsvUtils {
    fun read(reader: Reader): List<Array<String>> =
        CSVReader(reader).use { it.readAll() }

    fun write(writer: Writer, rows: List<Array<String>>) {
        CSVWriter(writer).use { csv -> rows.forEach { csv.writeNext(it) } }
    }
}
