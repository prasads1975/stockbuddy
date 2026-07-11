package com.gigakin.stockbuddy.util

import org.json.JSONObject

/**
 * FR-02 / OQ-01: the agreed QR payload for Individual Linking.
 * The QR code encodes a JSON object:
 * ```
 * { "productName": "...", "barcode": "...", "category": "...",
 *   "rfid": "...", "attributes": { "<fieldKey>": "<value>", ... } }
 * ```
 * `attributes` (domain-specific fields, Section 1.7) is optional; any of the fixed fields may be
 * absent (FR-04 partial payload) — the user reviews and completes on the Individual Linking form.
 */
data class QrPayload(
    val productName: String,
    val barcode: String,
    val category: String,
    val rfid: String,
    val attributes: Map<String, String>
)

object QrPayloadParser {
    /** Parses a decoded QR string into a [QrPayload], or null if it isn't valid JSON. */
    fun parse(raw: String): QrPayload? = try {
        val obj = JSONObject(raw)
        val attrs = mutableMapOf<String, String>()
        obj.optJSONObject("attributes")?.let { a ->
            a.keys().forEach { key -> attrs[key] = a.optString(key) }
        }
        QrPayload(
            productName = obj.optString("productName"),
            barcode = obj.optString("barcode"),
            category = obj.optString("category"),
            rfid = obj.optString("rfid"),
            attributes = attrs
        )
    } catch (e: Exception) {
        null
    }

    /** Serializes attributes back to a JSON string for passing as a navigation argument. */
    fun attributesToJson(attributes: Map<String, String>): String =
        JSONObject(attributes as Map<*, *>).toString()

    /** Reads an attributes JSON string (nav arg) back into a map. */
    fun attributesFromJson(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { key -> map[key] = obj.optString(key) }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
