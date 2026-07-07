package com.gigakin.stockbuddy.util

/**
 * Domain-specific field data types (FR-77j). The canonical value stored in
 * `FieldDefinitionEntity.type` is the enum NAME (e.g. "NUMBER"); the picker shows [label].
 * This is the single source of truth for the type picker and the stored value — keep the
 * two in sync so rendering checks (`type == "NUMBER"` / `"DROPDOWN"`) always match.
 */
enum class FieldType(val label: String) {
    TEXT("Text"),
    NUMBER("Number"),
    DROPDOWN("Dropdown"),
    DATE("Date");

    companion object {
        /** Display labels for the type picker, in declaration order. */
        val labels: List<String> = values().map { it.label }

        /** Picker label ("Number") → canonical stored value ("NUMBER"); defaults to TEXT. */
        fun valueForLabel(label: String): String =
            values().firstOrNull { it.label.equals(label, ignoreCase = true) }?.name ?: TEXT.name

        /** Stored value ("NUMBER", or legacy "Number"/"Currency") → display label; defaults to "Text". */
        fun labelForValue(value: String): String =
            values().firstOrNull { it.name.equals(value, ignoreCase = true) }?.label ?: TEXT.label

        const val DROPDOWN_VALUE = "DROPDOWN"
    }
}

/** Three-state hardware reader status (FR-81). */
enum class ReaderStatus { CONNECTED, NOT_CONNECTED, NOT_AVAILABLE }

/** Demo Mode Limits enforcement result (Section 4, MVP Scope doc). */
sealed class LimitCheck {
    object Ok : LimitCheck()
    data class Exceeded(val message: String) : LimitCheck()
}
