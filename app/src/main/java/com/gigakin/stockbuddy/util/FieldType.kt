package com.gigakin.stockbuddy.util

/** Domain-specific field data types (FR-77j). */
enum class FieldType { TEXT, NUMBER, DROPDOWN, DATE }

/** Article ID usage modes (Section 1.7, NFR-56, FR-77o). Default is OPTIONAL. */
enum class ArticleIdMode { NOT_USED, OPTIONAL, MANDATORY }

/** Three-state hardware reader status (FR-81). */
enum class ReaderStatus { CONNECTED, NOT_CONNECTED, NOT_AVAILABLE }

/** Demo Mode Limits enforcement result (Section 4, MVP Scope doc). */
sealed class LimitCheck {
    object Ok : LimitCheck()
    data class Exceeded(val message: String) : LimitCheck()
}
