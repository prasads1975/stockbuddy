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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
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
    private var fieldDefs: List<FieldDefinitionEntity> = emptyList()
    private val dynamicFieldViews = mutableMapOf<String, TextInputLayout>()
    private val dynamicEditTexts = mutableMapOf<String, TextInputEditText>()

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

        app.fieldConfigRepository.observeFields().observe(viewLifecycleOwner) { defs ->
            fieldDefs = defs
            editingItem?.let { renderDynamicFields(it) }
        }

        viewModel.categoryRepository.observeAll().observe(viewLifecycleOwner) { cats ->
            val categoryNames = cats.map { it.name }
            val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            (binding.editAssetCategory as? AutoCompleteTextView)?.setAdapter(categoryAdapter)
        }

        binding.btnCloseDialog.setOnClickListener { dismiss() }
        binding.btnCancelDialog.setOnClickListener { dismiss() }

        binding.btnSaveChanges.setOnClickListener {
            editingItem?.let { item ->
                if (validateForm()) {
                    val updated = captureFormData(item)
                    viewModel.updateItem(updated)
                    dismiss()
                }
            }
        }
    }

    private fun populateForm(item: LinkedItemEntity) {
        binding.editAssetName.setText(item.productName)
        binding.editAssetBarcode.setText(item.barcode)
        binding.editAssetCategory.setText(item.category)
    }

    private fun renderDynamicFields(item: LinkedItemEntity) {
        binding.dynamicFieldsContainer.removeAllViews()
        dynamicFieldViews.clear()
        dynamicEditTexts.clear()

        val attrs = JsonAttributes.toMap(item.attributesJson)
        val fieldsToShow = fieldDefs.filter { it.showOnLinking }

        fieldsToShow.forEach { fieldDef ->
            val value = attrs[fieldDef.key] ?: ""
            val inputLayout = TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
                hint = fieldDef.label
                helperText = if (fieldDef.mandatory) "Required" else null
            }

            val editText = TextInputEditText(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setText(value)
            }

            inputLayout.addView(editText)
            binding.dynamicFieldsContainer.addView(inputLayout)
            dynamicFieldViews[fieldDef.key] = inputLayout
            dynamicEditTexts[fieldDef.key] = editText
            android.util.Log.d("EditAsset", "Rendered field: ${fieldDef.key} = '$value', mandatory=${fieldDef.mandatory}")
        }
    }

    private fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()

        val name = binding.editAssetName.text?.toString()?.trim() ?: ""
        val barcode = binding.editAssetBarcode.text?.toString()?.trim() ?: ""
        val category = binding.editAssetCategory.text?.toString()?.trim() ?: ""

        if (name.isBlank()) errors["name"] = "Required"
        if (barcode.isBlank()) errors["barcode"] = "Required"
        if (category.isBlank()) errors["category"] = "Required"

        android.util.Log.d("EditAsset", "Validating ${fieldDefs.size} fields, ${fieldDefs.filter { it.mandatory && it.showOnLinking }.size} are mandatory+showOnLinking")

        fieldDefs.filter { it.mandatory && it.showOnLinking }.forEach { fieldDef ->
            val editText = dynamicEditTexts[fieldDef.key]
            android.util.Log.d("EditAsset", "Checking field '${fieldDef.key}': editText=$editText")

            if (editText != null) {
                val value = editText.text?.toString()?.trim() ?: ""
                android.util.Log.d("EditAsset", "Field '${fieldDef.key}': value='$value', isBlank=${value.isBlank()}")

                if (value.isBlank()) {
                    errors["attr_${fieldDef.key}"] = "Required"
                }
            }
        }

        clearAllErrors()
        if (errors.isNotEmpty()) {
            android.util.Log.d("EditAsset", "Validation failed with errors: $errors")
            applyErrors(errors)
            return false
        }

        android.util.Log.d("EditAsset", "Validation passed")
        return true
    }

    private fun applyErrors(errors: Map<String, String>) {
        errors.forEach { (key, message) ->
            android.util.Log.d("EditAsset", "Applying error for '$key': $message")
            when (key) {
                "name" -> {
                    binding.layoutAssetName.error = message
                    android.util.Log.d("EditAsset", "Name error set: $message")
                }
                "barcode" -> {
                    binding.layoutAssetBarcode.error = message
                    android.util.Log.d("EditAsset", "Barcode error set: $message")
                }
                "category" -> {
                    binding.layoutAssetCategory.error = message
                    android.util.Log.d("EditAsset", "Category error set: $message")
                }
                else -> {
                    val fieldKey = key.removePrefix("attr_")
                    dynamicFieldViews[fieldKey]?.error = message
                    android.util.Log.d("EditAsset", "Custom field '$fieldKey' error set")
                }
            }
        }
    }

    private fun clearAllErrors() {
        binding.layoutAssetName.error = null
        binding.layoutAssetBarcode.error = null
        binding.layoutAssetCategory.error = null
        dynamicFieldViews.values.forEach { it.error = null }
    }

    private fun captureFormData(original: LinkedItemEntity): LinkedItemEntity {
        val attrs = mutableMapOf<String, String>()

        fieldDefs.forEach { fieldDef ->
            val editText = dynamicEditTexts[fieldDef.key]
            if (editText != null) {
                val value = editText.text?.toString()?.trim() ?: ""
                if (value.isNotBlank()) {
                    attrs[fieldDef.key] = value
                }
            }
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
