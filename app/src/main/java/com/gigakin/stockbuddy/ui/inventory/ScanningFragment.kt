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
            binding.tvScanCount.text = getString(R.string.scanned_count_format, count)
        }

        // FR-81a: disabled with a clear message when the reader isn't available, rather than
        // allowed to proceed and fail silently.
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            val available = status == ReaderStatus.CONNECTED
            binding.btnStart.isEnabled = available
            binding.btnStart.text = if (available) getString(R.string.action_start)
                else getString(R.string.action_start) + " (reader unavailable)"
        }

        binding.btnStart.setOnClickListener {
            viewModel.start(args.sessionId)
            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
        }
        binding.btnStop.setOnClickListener {
            viewModel.stop(args.sessionId)
            findNavController().navigate(
                ScanningFragmentDirections.actionScanningToResults(args.sessionId, args.sessionCode)
            )
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
