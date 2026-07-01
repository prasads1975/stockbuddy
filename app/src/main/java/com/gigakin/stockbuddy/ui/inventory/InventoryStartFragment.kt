package com.gigakin.stockbuddy.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentInventoryStartBinding
import com.gigakin.stockbuddy.util.ReaderStatus

/** S11 — Inventory Start Ready: Confirm session code, select category filter, start scanning (FR-29, FR-35). */
class InventoryStartFragment : Fragment() {
    private var _binding: FragmentInventoryStartBinding? = null
    private val binding get() = _binding!!
    private val args: InventoryStartFragmentArgs by navArgs()
    private val app get() = requireActivity().application as StockBuddyApp

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Display the session code passed from dialog
        binding.tvSessionCode.text = args.sessionCode

        // Back button navigation
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Load categories and populate dropdown
        app.categoryRepository.observeAll().observe(viewLifecycleOwner) { cats ->
            val names = listOf(getString(R.string.filter_all_categories)) + cats.map { it.name }
            binding.spinnerCategoryFilter.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                names
            )
        }

        // Reader status indicator
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

        // START SCANNING button — navigate to ScanningFragment without auto-start
        binding.btnStartScanning.setOnClickListener {
            findNavController().navigate(
                InventoryStartFragmentDirections.actionInventoryStartToScanning(
                    args.sessionId,
                    args.sessionCode,
                    autoStart = true
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
