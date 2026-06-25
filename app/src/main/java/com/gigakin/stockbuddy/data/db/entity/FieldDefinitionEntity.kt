package com.gigakin.stockbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** FR-77i-o / NFR-51-56: domain-specific field metadata, the headline dynamic-field feature. */
@Entity(tableName = "field_definitions")
data class FieldDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,                 // e.g. "price", "net_weight"
    val label: String,               // e.g. "Price", "Net Weight"
    val type: String,                // TEXT | NUMBER | DROPDOWN | DATE  (see util/FieldType.kt)
    val mandatory: Boolean = false,
    val dropdownOptionsCsv: String? = null, // comma-separated, only used when type == DROPDOWN
    val showOnLinking: Boolean = true,
    val showOnAssets: Boolean = true,
    val showOnInventory: Boolean = true,
    val showOnCsv: Boolean = true,
    val sortOrder: Int = 0
)
