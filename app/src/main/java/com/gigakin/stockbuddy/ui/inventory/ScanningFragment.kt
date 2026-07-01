package com.gigakin.stockbuddy.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentScanningBinding
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** S12 — Inventory Scanning: START/STOP (FR-33/36), real-time count (FR-34), category filter (FR-35). */
class ScanningFragment : Fragment() {
    private var _binding: FragmentScanningBinding? = null
    private val binding get() = _binding!!
    private val args: ScanningFragmentArgs by navArgs()
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: ScanningViewModel by viewModels {
        ViewModelFactory { ScanningViewModel(app.inventoryRepository, app.categoryRepository, app.scannerManager) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScanningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvSessionCode.text = args.sessionCode

        viewModel.categories.observe(viewLifecycleOwner) { cats ->
            val names = listOf(getString(R.string.filter_all_categories)) + cats.map { it.name }
            binding.spinnerCategoryFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
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
            } else {
                binding.btnStart.visibility = View.VISIBLE
                binding.btnStop.visibility = View.GONE
                binding.summaryCardsContainer.visibility = View.GONE
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
            viewModel.stop(args.sessionId)
            findNavController().navigate(
                ScanningFragmentDirections.actionScanningToResults(args.sessionId, args.sessionCode)
            )
        }

        // Auto-start scanning if coming from InventoryStartFragment
        if (args.autoStart) {
            viewModel.start(args.sessionId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
