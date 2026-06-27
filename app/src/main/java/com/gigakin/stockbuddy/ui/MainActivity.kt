package com.gigakin.stockbuddy.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.ActivityMainBinding
import com.gigakin.stockbuddy.util.ReaderStatus
import com.google.android.material.card.MaterialCardView

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
        val (text, colorRes) = when (status) {
            ReaderStatus.CONNECTED -> Pair(getString(R.string.reader_connected), R.color.reader_connected)
            ReaderStatus.NOT_CONNECTED -> Pair(getString(R.string.reader_not_connected), R.color.reader_not_connected)
            ReaderStatus.NOT_AVAILABLE -> Pair(getString(R.string.reader_not_available), R.color.reader_not_available)
        }

        // Update text
        val statusText = binding.readerStatusBar.findViewById<TextView>(R.id.readerStatusText)
        statusText?.text = text

        // Find the MaterialCardView inside the container and update its background color
        val statusContainer = binding.root.findViewById<android.widget.FrameLayout>(R.id.readerStatusBarContainer)
        statusContainer?.let {
            val cardView = it.getChildAt(0) as? MaterialCardView
            cardView?.setCardBackgroundColor(getColor(colorRes))
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.nav_host_fragment).navigateUp() || super.onSupportNavigateUp()
}
