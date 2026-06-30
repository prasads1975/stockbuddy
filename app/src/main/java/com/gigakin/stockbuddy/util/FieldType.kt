package com.gigakin.stockbuddy.util

/** Domain-specific field data types (FR-77j). */
enum class FieldType { TEXT, NUMBER, DROPDOWN, DATE }

/** Three-state hardware reader status (FR-81). */
enum class ReaderStatus { CONNECTED, NOT_CONNECTED, NOT_AVAILABLE }

/** Demo Mode Limits enforcement result (Section 4, MVP Scope doc). */
sealed class LimitCheck {
    object Ok : LimitCheck()
    data class Exceeded(val message: String) : LimitCheck()
}
