package com.gigakin.stockbuddy.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentScanningBinding
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/** S12 — Inventory Scanning: START/STOP (FR-33/36), real-time count (FR-34), category filter (FR-35). */
class ScanningFragment : Fragment() {
    private var _binding: FragmentScanningBinding? = null
    private val binding get() = _binding!!
    private val args: ScanningFragmentArgs by navArgs()
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: ScanningViewModel by viewModels {
        ViewModelFactory { ScanningViewModel(app.inventoryRepository, app.categoryRepository, app.scannerManager) }
    }

    private var categories: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScanningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvSessionCode.text = args.sessionCode

        viewModel.categories.observe(viewLifecycleOwner) { cats ->
            categories = listOf(getString(R.string.filter_all_categories)) + cats.map { it.name }
        }

        viewModel.scanCount.observe(viewLifecycleOwner) { count ->
            binding.tvScanCount.text = count.toString()
        }

        // Observe scan stats and update summary cards
        viewModel.scanStats.observe(viewLifecycleOwner) { stats ->
            binding.tvAvailableCount.text = stats.available.toString()
            binding.tvMissingCount.text = stats.missing.toString()
            binding.tvExcessCount.text = stats.excess.toString()
        }

        // Observe scanning state to show/hide appropriate button and summary cards
        viewModel.scanning.observe(viewLifecycleOwner) { isScanning ->
            if (isScanning) {
                binding.btnStart.visibility = View.GONE
                binding.btnStop.visibility = View.VISIBLE
                binding.summaryCardsContainer.visibility = View.VISIBLE
                // Start progress bar animation
                startProgressBarAnimation()
            } else {
                binding.btnStart.visibility = View.VISIBLE
                binding.btnStop.visibility = View.GONE
                binding.summaryCardsContainer.visibility = View.GONE
                // Cancel progress bar animation
                binding.progressBarFill.animate().cancel()
                binding.progressBarFill.translationX = 0f
            }
        }

        // Reader status indicator
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            // Button always enabled - uses real scanning if available, automatic simulation if not
            binding.btnStart.isEnabled = true
            binding.btnStart.text = getString(R.string.action_start)

            // Update reader status text and icon
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
            // Update status icon color
            binding.readerStatusIcon.setColorFilter(requireContext().getColor(statusColor))
        }

        binding.btnStart.setOnClickListener {
            viewModel.start(args.sessionId)
        }

        binding.btnStop.setOnClickListener {
            binding.btnStop.isEnabled = false
            viewModel.stop(args.sessionId)   // navigation happens once the snapshot is written
        }

        // Navigate to Results only after STOP has persisted the results snapshot.
        viewModel.stopped.observe(viewLifecycleOwner) { stoppedSessionId ->
            if (stoppedSessionId != null) {
                viewModel.consumeStopped()
                findNavController().navigate(
                    ScanningFragmentDirections.actionScanningToResults(args.sessionId, args.sessionCode)
                )
            }
        }

        // Category filter dropdown
        binding.btnCategoryFilter.setOnClickListener { button ->
            showCategoryDropdown(button)
        }

        // Auto-start scanning if coming from InventoryStartFragment
        if (args.autoStart) {
            viewModel.start(args.sessionId)
        }
    }

    private fun showCategoryDropdown(anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        categories.forEachIndexed { index, category ->
            popup.menu.add(0, index, index, category)
        }
        popup.setOnMenuItemClickListener { item ->
            binding.btnCategoryFilter.text = categories[item.itemId]
            true
        }
        popup.show()
    }

    private fun startProgressBarAnimation() {
        // Only animate if binding still exists
        _binding?.let {
            it.progressBarFill.animate()
                .translationX(it.summaryCardsContainer.width.toFloat())
                .setDuration(2000)
                .withEndAction {
                    // Check if binding still exists before recursing
                    if (_binding != null) {
                        _binding?.progressBarFill?.translationX = 0f
                        startProgressBarAnimation()
                    }
                }
                .start()
        }
    }

    override fun onDestroyView() {
        _binding?.progressBarFill?.animate()?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
