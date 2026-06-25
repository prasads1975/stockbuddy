package com.gigakin.stockbuddy.util

import org.json.JSONObject

/** Helpers for the Section 1.7 hybrid data model: fixed columns + a single JSON attributes blob. */
object JsonAttributes {
    fun toMap(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        val obj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { k -> map[k] = obj.optString(k, "") }
        return map
    }

    fun fromMap(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }
}
