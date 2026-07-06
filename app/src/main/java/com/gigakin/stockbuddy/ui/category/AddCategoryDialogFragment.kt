package com.gigakin.stockbuddy.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.DialogAddCategoryBinding
import com.gigakin.stockbuddy.util.LimitCheck
import com.gigakin.stockbuddy.util.ViewModelFactory

class AddCategoryDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogAddCategoryBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: CategoryViewModel by viewModels {
        ViewModelFactory { CategoryViewModel(app.categoryRepository, app.productRepository) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnCancel.setOnClickListener { dismiss() }

        // Observe add result to handle success/error — consume ONLY after processing, not in the callback
        viewModel.addResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe

            when (result) {
                is LimitCheck.Exceeded -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
                is LimitCheck.Ok -> {
                    dismiss()
                }
            }

            // Consume AFTER the when block completes and dismiss/snackbar are fully queued
            viewModel.consumeAddResult()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.editCategoryName.text?.toString()?.trim().orEmpty()
            if (name.isNotBlank()) {
                viewModel.add(name)
            } else {
                binding.editCategoryName.error = "Category name is required"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
