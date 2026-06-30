package com.gigakin.stockbuddy.ui.results

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.repo.InventoryRepository
import com.gigakin.stockbuddy.databinding.FragmentResultsBinding
import com.gigakin.stockbuddy.ui.export.ExportBottomSheetFragment
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvSessionTitle.text = getString(R.string.results_title_format, args.sessionCode, "")

        adapter = ResultGroupAdapter(requireContext())
        binding.recyclerResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerResults.adapter = adapter
        binding.recyclerResults.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_available))
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_missing))
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_excess))

        viewModel.categoryRepository.observeAll().observe(viewLifecycleOwner) { cats ->
            val names = listOf(getString(R.string.filter_all_categories)) + cats.map { it.name }
            binding.spinnerCategoryFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            binding.spinnerCategoryFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                    viewModel.setCategoryFilter(if (position == 0) null else names[position])
                    renderCurrentTab()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) = renderCurrentTab()
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewModel.filteredResults.observe(viewLifecycleOwner) { renderCurrentTab() }
        viewModel.load(args.sessionId)

        binding.btnExport.setOnClickListener {
            ExportBottomSheetFragment.newInstance(args.sessionId, args.sessionCode)
                .show(parentFragmentManager, "export")
        }
    }

    private fun renderCurrentTab() {
        val results = viewModel.currentResults()

        // Update tab labels with counts
        val availableCount = results.count { it.status == InventoryRepository.Status.AVAILABLE }
        val missingCount = results.count { it.status == InventoryRepository.Status.MISSING }
        val excessCount = results.count { it.status == InventoryRepository.Status.EXCESS }

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
