package com.gigakin.stockbuddy.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.ActivityMainBinding
import com.gigakin.stockbuddy.util.ReaderStatus

/**
 * Single-Activity + Navigation Component host. Hosts the persistent global Reader status
 * bar (FR-81, Section 4.6.2) above the nav-hosted fragment, so it's visible on every screen
 * without each Fragment needing its own copy.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as StockBuddyApp
        app.scannerManager.status.observe(this) { status -> renderReaderStatus(status) }
    }

    private fun renderReaderStatus(status: ReaderStatus) {
        val (text, colorRes, icon) = when (status) {
            ReaderStatus.CONNECTED -> Triple(getString(R.string.reader_connected), R.color.reader_connected, R.drawable.ic_reader_connected)
            ReaderStatus.NOT_CONNECTED -> Triple(getString(R.string.reader_not_connected), R.color.reader_not_connected, R.drawable.ic_reader_warning)
            ReaderStatus.NOT_AVAILABLE -> Triple(getString(R.string.reader_not_available), R.color.reader_not_available, R.drawable.ic_reader_unavailable)
        }
        binding.readerStatusBar.text = text
        binding.readerStatusBar.setTextColor(getColor(colorRes))
        binding.readerStatusBar.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.nav_host_fragment).navigateUp() || super.onSupportNavigateUp()
}
