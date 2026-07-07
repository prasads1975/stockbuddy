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
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.db.entity.ProductMasterEntity
import com.gigakin.stockbuddy.databinding.DialogEditAssetBinding
import com.gigakin.stockbuddy.util.JsonAttributes
import com.gigakin.stockbuddy.util.ViewModelFactory
import kotlinx.coroutines.launch

/** Product-level edit (v2, §4.0.6): edits product_master (name, category, attributes). Barcode is the PK (read-only). */
class EditAssetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogEditAssetBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: AssetsViewModel by viewModels {
        ViewModelFactory { AssetsViewModel(app.productRepository, app.categoryRepository) }
    }

    private var editingProduct: ProductMasterEntity? = null
    private var fieldDefs: List<FieldDefinitionEntity> = emptyList()
    private val dynamicFieldViews = mutableMapOf<String, TextInputLayout>()
    // EditText supertype so both TextInputEditText (text/number) and AutoCompleteTextView (dropdown) fit.
    private val dynamicEditTexts = mutableMapOf<String, android.widget.EditText>()

    companion object {
        private const val ARG_BARCODE = "barcode"
        fun newInstance(barcode: String) = EditAssetDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_BARCODE, barcode) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEditAssetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val barcode = arguments?.getString(ARG_BARCODE)
        if (barcode != null) {
            lifecycleScope.launch {
                val product = app.productRepository.getByBarcode(barcode)
                editingProduct = product
                if (product != null) {
                    val catName = app.categoryRepository.getAll().firstOrNull { it.id == product.categoryId }?.name ?: ""
                    populateForm(product, catName)
                    renderDynamicFields(product)
                }
            }
        }

        app.fieldConfigRepository.observeFields().observe(viewLifecycleOwner) { defs ->
            fieldDefs = defs
            editingProduct?.let { renderDynamicFields(it) }
        }

        viewModel.categoryRepository.observeAll().observe(viewLifecycleOwner) { cats ->
            val categoryNames = cats.map { it.name }
            val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            (binding.editAssetCategory as? AutoCompleteTextView)?.setAdapter(categoryAdapter)
        }

        binding.btnCloseDialog.setOnClickListener { dismiss() }
        binding.btnCancelDialog.setOnClickListener { dismiss() }

        binding.btnSaveChanges.setOnClickListener {
            val product = editingProduct ?: return@setOnClickListener
            if (validateForm()) {
                val name = binding.editAssetName.text?.toString()?.trim().orEmpty()
                val category = binding.editAssetCategory.text?.toString()?.trim().orEmpty()
                viewModel.updateProduct(product.barcode, name, category, captureAttrs()) { ok ->
                    if (ok) dismiss()
                    else binding.layoutAssetCategory.error = "Category doesn't exist"
                }
            }
        }
    }

    private fun populateForm(product: ProductMasterEntity, categoryName: String) {
        binding.editAssetName.setText(product.productName)
        binding.editAssetBarcode.setText(product.barcode)
        binding.editAssetBarcode.isEnabled = false   // barcode is the PK — not editable
        binding.editAssetCategory.setText(categoryName)
    }

    private fun renderDynamicFields(product: ProductMasterEntity) {
        binding.dynamicFieldsContainer.removeAllViews()
        dynamicFieldViews.clear()
        dynamicEditTexts.clear()

        val attrs = JsonAttributes.toMap(product.attributesJson)
        val fieldsToShow = fieldDefs.filter { it.showOnLinking }
        val fieldMargins = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 }

        fieldsToShow.forEach { fieldDef ->
            val value = attrs[fieldDef.key] ?: ""
            val input: android.widget.EditText
            val inputLayout: TextInputLayout

            if (fieldDef.type == "DROPDOWN") {
                // Outlined exposed-dropdown-menu (box + chevron), matching the Linking form.
                inputLayout = TextInputLayout(
                    requireContext(), null,
                    com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
                ).apply {
                    layoutParams = fieldMargins
                    hint = fieldDef.label
                    helperText = if (fieldDef.mandatory) "Required" else null
                }
                val options = fieldDef.dropdownOptionsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                input = AutoCompleteTextView(requireContext()).apply {
                    inputType = android.text.InputType.TYPE_NULL
                    keyListener = null
                    setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options))
                    setText(value, false)   // preselect current value without filtering
                }
            } else {
                inputLayout = TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
                    layoutParams = fieldMargins
                    hint = fieldDef.label
                    helperText = if (fieldDef.mandatory) "Required" else null
                }
                input = TextInputEditText(requireContext()).apply {
                    inputType = if (fieldDef.type == "NUMBER") android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                                else android.text.InputType.TYPE_CLASS_TEXT
                    setText(value)
                }
            }
            inputLayout.addView(input)
            binding.dynamicFieldsContainer.addView(inputLayout)
            dynamicFieldViews[fieldDef.key] = inputLayout
            dynamicEditTexts[fieldDef.key] = input
        }
    }

    private fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()
        val name = binding.editAssetName.text?.toString()?.trim() ?: ""
        val category = binding.editAssetCategory.text?.toString()?.trim() ?: ""
        if (name.isBlank()) errors["name"] = "Required"
        if (category.isBlank()) errors["category"] = "Required"

        fieldDefs.filter { it.mandatory && it.showOnLinking }.forEach { fieldDef ->
            val value = dynamicEditTexts[fieldDef.key]?.text?.toString()?.trim() ?: ""
            if (value.isBlank()) errors["attr_${fieldDef.key}"] = "Required"
        }

        clearAllErrors()
        if (errors.isNotEmpty()) { applyErrors(errors); return false }
        return true
    }

    private fun applyErrors(errors: Map<String, String>) {
        errors.forEach { (key, message) ->
            when (key) {
                "name" -> binding.layoutAssetName.error = message
                "category" -> binding.layoutAssetCategory.error = message
                else -> dynamicFieldViews[key.removePrefix("attr_")]?.error = message
            }
        }
    }

    private fun clearAllErrors() {
        binding.layoutAssetName.error = null
        binding.layoutAssetBarcode.error = null
        binding.layoutAssetCategory.error = null
        dynamicFieldViews.values.forEach { it.error = null }
    }

    private fun captureAttrs(): String {
        val attrs = mutableMapOf<String, String>()
        fieldDefs.forEach { fieldDef ->
            val value = dynamicEditTexts[fieldDef.key]?.text?.toString()?.trim() ?: ""
            if (value.isNotBlank()) attrs[fieldDef.key] = value
        }
        return JsonAttributes.fromMap(attrs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
