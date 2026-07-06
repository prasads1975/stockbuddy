package com.gigakin.stockbuddy.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.CategoryEntity
import com.gigakin.stockbuddy.databinding.DialogEditCategoryBinding
import com.gigakin.stockbuddy.util.ViewModelFactory

class EditCategoryDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogEditCategoryBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: CategoryViewModel by viewModels {
        ViewModelFactory { CategoryViewModel(app.categoryRepository, app.productRepository) }
    }

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_CATEGORY_NAME = "category_name"

        fun newInstance(id: Long, name: String) = EditCategoryDialogFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_CATEGORY_ID, id)
                putString(ARG_CATEGORY_NAME, name)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val categoryName = arguments?.getString(ARG_CATEGORY_NAME) ?: ""
        val categoryId = arguments?.getLong(ARG_CATEGORY_ID) ?: 0L

        binding.editCategoryName.setText(categoryName)

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val name = binding.editCategoryName.text?.toString()?.trim().orEmpty()
            if (name.isNotBlank()) {
                viewModel.update(CategoryEntity(id = categoryId, name = name))
                dismiss()
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
