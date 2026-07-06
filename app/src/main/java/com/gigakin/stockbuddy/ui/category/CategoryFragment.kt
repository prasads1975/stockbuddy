package com.gigakin.stockbuddy.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.databinding.FragmentCategoryBinding
import com.gigakin.stockbuddy.util.LimitCheck
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/** S17 — Category Management (FR-72/73), with the demo cap (Section 4, MVP Scope doc). */
class CategoryFragment : Fragment() {
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: CategoryViewModel by viewModels {
        ViewModelFactory { CategoryViewModel(app.categoryRepository, app.productRepository) }
    }
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = CategoryAdapter(
            onEdit = { cat -> showEditDialog(cat) },
            onDelete = { cat -> showDeleteConfirmation(cat) }
        )
        binding.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCategories.adapter = adapter

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            android.util.Log.d("CategoryFragment", "Observer received ${categories.size} categories: ${categories.map { it.name }}")
            adapter.submitList(categories)
        }

        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        binding.btnAddCategory.setOnClickListener { showAddDialog() }

        binding.editSearchCategory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.addResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is LimitCheck.Exceeded -> Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                is LimitCheck.Ok -> {}
                null -> {}
            }
            viewModel.consumeAddResult()
        }

        observeReaderStatus()
    }

    private fun observeReaderStatus() {
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
            binding.iconReaderStatus.setColorFilter(requireContext().getColor(statusColor))
        }
    }

    private fun showAddDialog() {
        AddCategoryDialogFragment().show(parentFragmentManager, "add_category")
    }

    private fun showEditDialog(category: CategoryEntity) {
        EditCategoryDialogFragment.newInstance(category.id, category.name)
            .show(parentFragmentManager, "edit_category")
    }

    private fun showDeleteConfirmation(category: CategoryEntity) {
        // Block + warn with cascade impact count (§4.0.6).
        viewModel.productCount(category.id) { count ->
            if (!isAdded) return@productCount
            val message = if (count > 0)
                getString(R.string.delete_category_confirm, category.name, count)
            else
                getString(R.string.dialog_delete_category_message)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_delete_category_title))
                .setMessage(message)
                .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(getString(R.string.action_delete)) { _, _ -> viewModel.delete(category) }
                .show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
