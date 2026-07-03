package com.gigakin.stockbuddy.ui.assets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.LinkedItemEntity
import com.gigakin.stockbuddy.databinding.DialogEditAssetBinding
import com.gigakin.stockbuddy.util.JsonAttributes
import com.gigakin.stockbuddy.util.ViewModelFactory
import kotlinx.coroutines.launch

class EditAssetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogEditAssetBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: AssetsViewModel by viewModels {
        ViewModelFactory { AssetsViewModel(app.itemRepository, app.categoryRepository) }
    }

    private var editingItem: LinkedItemEntity? = null

    companion object {
        private const val ARG_RFID = "rfid"

        fun newInstance(item: LinkedItemEntity) = EditAssetDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_RFID, item.rfidTagId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEditAssetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rfidTagId = arguments?.getString(ARG_RFID)
        if (rfidTagId != null) {
            lifecycleScope.launch {
                editingItem = app.itemRepository.getByRfid(rfidTagId)
                editingItem?.let { populateForm(it) }
            }
        }

        // Load categories for dropdown
        viewModel.categoryRepository.observeAll().observe(viewLifecycleOwner) { cats ->
            val categoryNames = cats.map { it.name }
            val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            (binding.editAssetCategory as? AutoCompleteTextView)?.setAdapter(categoryAdapter)
        }

        binding.btnCloseDialog.setOnClickListener { dismiss() }
        binding.btnCancelDialog.setOnClickListener { dismiss() }

        binding.btnSaveChanges.setOnClickListener {
            editingItem?.let { item ->
                val updated = captureFormData(item)
                viewModel.updateItem(updated)
                dismiss()
            }
        }
    }

    private fun populateForm(item: LinkedItemEntity) {
        binding.editAssetName.setText(item.productName)
        binding.editAssetBarcode.setText(item.barcode)
        binding.editAssetCategory.setText(item.category)

        val attrs = JsonAttributes.toMap(item.attributesJson)
        binding.editAssetPrice.setText(attrs["Price"] ?: "")
    }

    private fun captureFormData(original: LinkedItemEntity): LinkedItemEntity {
        val attrs = JsonAttributes.toMap(original.attributesJson).toMutableMap()
        val price = binding.editAssetPrice.text?.toString()
        if (!price.isNullOrBlank()) {
            attrs["Price"] = price
        }

        return original.copy(
            productName = binding.editAssetName.text?.toString() ?: original.productName,
            barcode = binding.editAssetBarcode.text?.toString() ?: original.barcode,
            category = binding.editAssetCategory.text?.toString() ?: original.category,
            attributesJson = JsonAttributes.fromMap(attrs)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
