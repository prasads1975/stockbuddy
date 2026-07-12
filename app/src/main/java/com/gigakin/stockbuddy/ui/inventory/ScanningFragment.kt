package com.gigakin.stockbuddy.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentScanningBinding
import com.gigakin.stockbuddy.util.ReaderStatusBar
import com.gigakin.stockbuddy.util.ViewModelFactory

/** S12 — Inventory Scanning: START/STOP (FR-33/36), real-time count (FR-34), category filter (FR-35, locked). */
class ScanningFragment : Fragment() {
    private var _binding: FragmentScanningBinding? = null
    private val binding get() = _binding!!
    private val args: ScanningFragmentArgs by navArgs()
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: ScanningViewModel by viewModels {
        ViewModelFactory { ScanningViewModel(app.inventoryRepository, app.scannerManager) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScanningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvSessionCode.text = args.sessionCode

        viewModel.categoryFilter.observe(viewLifecycleOwner) { category ->
            binding.tvCategoryLocked.text = category ?: getString(R.string.filter_all_categories)
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

            // Reader status bar (FR-81) — centralised in ReaderStatusBar.
            ReaderStatusBar.bind(status, binding.readerStatusBar, binding.readerStatusIcon, binding.tvReaderStatus)
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

        // Auto-start scanning if coming from InventoryStartFragment
        if (args.autoStart) {
            viewModel.start(args.sessionId)
        }
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
