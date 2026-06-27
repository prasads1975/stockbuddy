package com.gigakin.stockbuddy.ui.linking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentLinkingOptionsBinding
import com.gigakin.stockbuddy.util.ReaderStatus

/** S04 — choice between Individual Linking and Bulk Linking. QR mode (FR-01-04) is cut from MVP. */
class LinkingOptionsFragment : Fragment() {
    private var _binding: FragmentLinkingOptionsBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLinkingOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Back button navigation
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Card click listeners
        binding.btnIndividual.setOnClickListener {
            findNavController().navigate(LinkingOptionsFragmentDirections.actionToIndividualLinking())
        }
        binding.btnBulk.setOnClickListener {
            findNavController().navigate(LinkingOptionsFragmentDirections.actionToBulkLinking())
        }

        // Observe reader status for hardware status bar
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            val statusText = when (status) {
                ReaderStatus.CONNECTED -> getString(R.string.reader_connected)
                ReaderStatus.NOT_CONNECTED -> getString(R.string.reader_not_connected)
                ReaderStatus.NOT_AVAILABLE -> getString(R.string.reader_not_available)
            }
            binding.tvReaderStatus.text = statusText

            val statusColor = when (status) {
                ReaderStatus.CONNECTED -> R.color.status_available
                ReaderStatus.NOT_CONNECTED -> R.color.md_theme_error
                ReaderStatus.NOT_AVAILABLE -> R.color.md_theme_onSurfaceVariant
            }
            binding.readerStatusIcon.setColorFilter(requireContext().getColor(statusColor))
        }
    }

    override fun onResume() {
        super.onResume()
        // Hide the activity's header and status bar since we show our own header
        binding.root.post {
            try {
                val headerToolbar = requireActivity().findViewById<View>(R.id.toolbarHeader)
                headerToolbar?.visibility = View.GONE

                val readerStatusBar = requireActivity().findViewById<View>(R.id.readerStatusBarContainer)
                readerStatusBar?.visibility = View.GONE
            } catch (e: Exception) {
                // Silently ignore if views not found
            }
        }
    }

    override fun onDestroyView() {
        // Restore the activity's header and status bar when leaving this screen
        val headerToolbar = requireActivity().findViewById<View>(R.id.toolbarHeader)
        headerToolbar?.visibility = View.VISIBLE

        val readerStatusBar = requireActivity().findViewById<View>(R.id.readerStatusBarContainer)
        readerStatusBar?.visibility = View.VISIBLE

        super.onDestroyView()
        _binding = null
    }
}
