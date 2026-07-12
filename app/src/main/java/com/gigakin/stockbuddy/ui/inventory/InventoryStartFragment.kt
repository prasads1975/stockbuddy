package com.gigakin.stockbuddy.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.databinding.FragmentInventoryStartBinding
import com.gigakin.stockbuddy.util.ReaderStatusBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** S11 — Inventory Start Ready: Confirm session code, select category filter, start scanning (FR-29, FR-35). */
class InventoryStartFragment : Fragment() {
    private var _binding: FragmentInventoryStartBinding? = null
    private val binding get() = _binding!!
    private val args: InventoryStartFragmentArgs by navArgs()
    private val app get() = requireActivity().application as StockBuddyApp
    private var filterCategories: List<CategoryEntity> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Display the inventory code passed from dialog (not yet saved to DB)
        binding.tvSessionCode.text = args.inventoryCode

        // Back button navigation
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Load categories and populate dropdown
        app.categoryRepository.observeAll().observe(viewLifecycleOwner) { cats ->
            filterCategories = cats
            val names = listOf(getString(R.string.filter_all_categories)) + cats.map { it.name }
            binding.spinnerCategoryFilter.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                names
            )
        }

        // Reader status indicator (FR-81) — centralised in ReaderStatusBar.
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            ReaderStatusBar.bind(status, binding.readerStatusBar, binding.readerStatusIcon, binding.tvReaderStatus)
        }

        // START SCANNING button — create session and then start scanning
        binding.btnStartScanning.setOnClickListener {
            createSessionAndStartScanning()
        }
    }

    private fun createSessionAndStartScanning() {
        lifecycleScope.launch {
            try {
                binding.btnStartScanning.isEnabled = false
                // Position 0 is "All" (no filter); otherwise resolve to the selected category name (FR-35).
                val position = binding.spinnerCategoryFilter.selectedItemPosition
                val categoryFilter = if (position <= 0) null else filterCategories.getOrNull(position - 1)?.name
                val sessionId = withContext(Dispatchers.IO) {
                    app.inventoryRepository.startSession(args.inventoryCode, categoryFilter)
                }
                findNavController().navigate(
                    InventoryStartFragmentDirections.actionInventoryStartToScanning(
                        sessionId,
                        args.inventoryCode,
                        autoStart = true
                    )
                )
            } catch (e: Exception) {
                binding.btnStartScanning.isEnabled = true
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
