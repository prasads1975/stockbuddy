package com.gigakin.stockbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App config (§4.2.5): key-value configuration store for app-wide settings.
 * PK: key (TEXT). Examples: "theme_mode", "last_session_id", "field_config_completed".
 */
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)
