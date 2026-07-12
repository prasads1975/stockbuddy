package com.gigakin.stockbuddy.util

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.gigakin.stockbuddy.R

/**
 * Single source of truth for the persistent reader-status bar (FR-81) shown on every screen.
 *
 * This replaces what were 13 divergent inline implementations across the fragments (which had
 * drifted to three different NOT_CONNECTED colours: amber, red, and android-orange). Each state is
 * always paired with an icon + text label, so colour is never the sole signal (NFR-11).
 *
 * @param bar  the full-width bar container (its background is set per state)
 * @param icon the sensors icon
 * @param text the status label
 */
object ReaderStatusBar {

    private data class Style(val barColor: Int, val foreground: Int, val label: Int)

    fun bind(status: ReaderStatus, bar: View, icon: ImageView, text: TextView) {
        val s = styleFor(status)
        val ctx = bar.context
        val fg = ContextCompat.getColor(ctx, s.foreground)
        bar.setBackgroundColor(ContextCompat.getColor(ctx, s.barColor))
        icon.setColorFilter(fg)
        text.setTextColor(fg)
        text.setText(s.label)
    }

    /**
     * Asymmetric emphasis — calm when fine, loud only when actually broken:
     *  - CONNECTED:     neutral bar, green icon + text (low emphasis; the expected default).
     *  - NOT_CONNECTED: filled soft-red (errorContainer) bar with dark-red icon + text — the one
     *                   fault state worth interrupting the user.
     *  - NOT_AVAILABLE: neutral bar, grey icon + text. This is the emulator / non-C72 state — it is
     *                   informational, NOT an error, so it is deliberately not alarmed in red.
     * Colour is always paired with the text label (NFR-11); the text now carries the status colour
     * too, so the signal is legible at a glance rather than hidden in a small icon.
     */
    private fun styleFor(status: ReaderStatus): Style = when (status) {
        ReaderStatus.CONNECTED -> Style(
            barColor = R.color.surface_container_low,
            foreground = R.color.status_available,
            label = R.string.reader_connected
        )
        ReaderStatus.NOT_CONNECTED -> Style(
            barColor = R.color.md_theme_errorContainer,
            foreground = R.color.md_theme_onErrorContainer,
            label = R.string.reader_not_connected
        )
        ReaderStatus.NOT_AVAILABLE -> Style(
            barColor = R.color.surface_container_low,
            foreground = R.color.reader_not_available,
            label = R.string.reader_not_available
        )
    }
}
