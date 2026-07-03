package com.gigakin.stockbuddy.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.databinding.FragmentAssetsBinding
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/** S09 — Assets: flat list (FR-61), search (FR-63), category filter (FR-67), live count (FR-64). */
class AssetsFragment : Fragment() {
    private var _binding: FragmentAssetsBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: AssetsViewModel by viewModels {
        ViewModelFactory { AssetsViewModel(app.itemRepository, app.categoryRepository) }
    }
    private lateinit var adapter: AssetsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAssetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            val (colorRes, textRes) = when (status) {
                ReaderStatus.CONNECTED -> R.color.status_available to R.string.reader_connected
                ReaderStatus.NOT_CONNECTED -> R.color.status_excess to R.string.reader_not_connected
                ReaderStatus.NOT_AVAILABLE -> R.color.reader_not_available to R.string.reader_not_available
            }
            androidx.core.widget.ImageViewCompat.setImageTintList(
                binding.readerStatusIcon,
                androidx.core.content.res.ResourcesCompat.getColorStateList(requireContext().resources, colorRes, null)
            )
            binding.tvReaderStatus.text = getString(textRes)
        }

        adapter = AssetsAdapter(
            onEditClick = { item -> showEditDialog(item) },
            onDeleteClick = { item -> confirmAndDeleteItem(item) }
        )
        binding.recyclerAssets.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAssets.adapter = adapter

        binding.editSearch.addTextChangedListener { text -> viewModel.setQuery(text?.toString().orEmpty()) }

        viewModel.categoryRepository.observeAll().observe(viewLifecycleOwner) { cats ->
            val names = listOf(getString(R.string.filter_all_categories)) + cats.map { it.name }
            binding.spinnerCategoryFilter.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            binding.spinnerCategoryFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                    viewModel.setCategory(if (position == 0) null else names[position])
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.tvCount.text = getString(R.string.assets_count_format, items.size)
        }
    }

    private fun showEditDialog(item: LinkedItemEntity) {
        val dialog = EditAssetDialogFragment.newInstance(item)
        dialog.show(childFragmentManager, "edit_asset")
    }

    private fun confirmAndDeleteItem(item: LinkedItemEntity) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(R.string.delete_asset_confirm)
            .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteItem(item)
            }
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
