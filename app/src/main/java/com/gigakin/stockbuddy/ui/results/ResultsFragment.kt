package com.gigakin.stockbuddy.ui.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.data.repo.InventoryRepository
import com.gigakin.stockbuddy.databinding.FragmentResultsBinding
import com.gigakin.stockbuddy.ui.export.ExportBottomSheetFragment
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/** S13 — Results Summary: Available/Missing/Excess tabs (FR-43), grouped by Barcode (FR-45). */
class ResultsFragment : Fragment() {
    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!
    private val args: ResultsFragmentArgs by navArgs()
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: ResultsViewModel by viewModels {
        ViewModelFactory { ResultsViewModel(app.inventoryRepository, app.categoryRepository) }
    }

    private lateinit var adapter: ResultGroupAdapter
    private var filterCategories: List<CategoryEntity> = emptyList()
    // Guards against the ViewModel-driven spinner sync re-triggering setCategoryFilter (feedback loop).
    private var isApplyingProgrammaticSelection = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvTitle.text = getString(R.string.results_title)
        binding.chipSessionCode.text = args.sessionCode

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            binding.readerStatusIcon.setColorFilter(
                requireContext().getColor(
                    when (status) {
                        ReaderStatus.CONNECTED -> R.color.status_available
                        ReaderStatus.NOT_CONNECTED -> android.R.color.holo_orange_light
                        ReaderStatus.NOT_AVAILABLE -> R.color.md_theme_onSurfaceVariant
                    }
                )
            )
            binding.tvReaderStatus.text = when (status) {
                ReaderStatus.CONNECTED -> getString(R.string.reader_connected)
                ReaderStatus.NOT_CONNECTED -> getString(R.string.reader_not_connected)
                ReaderStatus.NOT_AVAILABLE -> getString(R.string.reader_not_available)
            }
            binding.tvReaderVersion.text = "UHF Reader v2.4"
        }

        adapter = ResultGroupAdapter(requireContext())
        binding.recyclerResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerResults.adapter = adapter
        binding.recyclerResults.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_available))
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_missing))
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_excess))

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) = renderCurrentTab()
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewModel.filteredResults.observe(viewLifecycleOwner) { renderCurrentTab() }

        setupCategoryFilter()
        viewModel.load(args.sessionId)

        binding.fabExport.setOnClickListener {
            ExportBottomSheetFragment.newInstance(args.sessionId, args.sessionCode)
                .show(parentFragmentManager, "export")
        }
    }

    /**
     * FR-46: the filter is only interactive when the session was scanned with "All" selected.
     * When scoped to a specific category, every other option would read 0/0 (see
     * ResultsViewModel.applyFilter), so a locked read-only label is shown instead.
     */
    private fun setupCategoryFilter() {
        app.categoryRepository.observeAll().observe(viewLifecycleOwner) { cats ->
            filterCategories = cats
            val names = listOf(getString(R.string.filter_all_categories)) + cats.map { it.name }
            binding.spinnerCategoryFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            syncSpinnerSelection()
        }

        binding.spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isApplyingProgrammaticSelection) return
                val category = if (position <= 0) null else filterCategories.getOrNull(position - 1)?.name
                viewModel.setCategoryFilter(category)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Reflects the Start-screen carry-over (FR-35) once the session loads, and any later manual change.
        viewModel.categoryFilter.observe(viewLifecycleOwner) { syncSpinnerSelection() }

        viewModel.sessionScope.observe(viewLifecycleOwner) { scope ->
            val isLocked = scope != null
            binding.spinnerCategoryFilter.visibility = if (isLocked) View.GONE else View.VISIBLE
            binding.tvCategoryLocked.visibility = if (isLocked) View.VISIBLE else View.GONE
            if (isLocked) binding.tvCategoryLocked.text = scope
        }
    }

    /** Sets the spinner's displayed position to match the ViewModel's authoritative filter, without re-triggering it. */
    private fun syncSpinnerSelection() {
        val filter = viewModel.categoryFilter.value
        val targetPosition = if (filter == null) 0 else {
            val index = filterCategories.indexOfFirst { it.name == filter }
            if (index >= 0) index + 1 else 0
        }
        if (binding.spinnerCategoryFilter.selectedItemPosition != targetPosition) {
            isApplyingProgrammaticSelection = true
            binding.spinnerCategoryFilter.setSelection(targetPosition)
            // Spinner defers onItemSelected to a posted Runnable rather than firing it synchronously,
            // so resetting the guard right here would race it. Post the reset to run after it instead.
            binding.spinnerCategoryFilter.post { isApplyingProgrammaticSelection = false }
        }
    }

    private fun renderCurrentTab() {
        val results = viewModel.currentResults()

        val availableCount = results.count { it.status == InventoryRepository.Status.AVAILABLE }
        val missingCount = results.count { it.status == InventoryRepository.Status.MISSING }
        val excessCount = results.count { it.status == InventoryRepository.Status.EXCESS }

        binding.tvAvailableCount.text = availableCount.toString()
        binding.tvMissingCount.text = missingCount.toString()
        binding.tvExcessCount.text = excessCount.toString()

        binding.tabs.getTabAt(0)?.text = "${getString(R.string.tab_available)} ($availableCount)"
        binding.tabs.getTabAt(1)?.text = "${getString(R.string.tab_missing)} ($missingCount)"
        binding.tabs.getTabAt(2)?.text = "${getString(R.string.tab_excess)} ($excessCount)"

        val status = when (binding.tabs.selectedTabPosition) {
            0 -> InventoryRepository.Status.AVAILABLE
            1 -> InventoryRepository.Status.MISSING
            else -> InventoryRepository.Status.EXCESS
        }
        adapter.submitList(results.filter { it.status == status })
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
