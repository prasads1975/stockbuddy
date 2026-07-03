package com.gigakin.stockbuddy.ui.linking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.databinding.FragmentIndividualLinkingBinding
import com.gigakin.stockbuddy.hardware.RfidScanResult
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/**
 * S06 — Manual Entry form (QR mode, FR-01-04, is cut from MVP; this is the only entry path).
 * Renders fixed fields (name, barcode, category, RFID) + all domain-specific fields
 * (including articleId) dynamically from field_definitions (NFR-52). Article ID is now
 * a configurable field stored in attributesJson, not a separate column. RFID/Barcode scan
 * use secondary container buttons (NFR-10f); Save Link = thumb-zone primary action (NFR-10c/d).
 */
class IndividualLinkingFragment : Fragment() {
    private var _binding: FragmentIndividualLinkingBinding? = null
    private val binding get() = _binding!!

    private val app get() = requireActivity().application as StockBuddyApp
    private val viewModel: IndividualLinkingViewModel by viewModels {
        ViewModelFactory {
            IndividualLinkingViewModel(app.itemRepository, app.fieldConfigRepository, app.categoryRepository, app.scannerManager)
        }
    }

    private val dynamicFieldViews = mutableMapOf<String, EditText>()
    private var currentFieldDefs: List<FieldDefinitionEntity> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIndividualLinkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Back button navigation
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.legendRequired.text = getString(R.string.legend_required)

        // Article ID field hidden (using barcode as the primary business identifier instead)
        binding.layoutArticleId.visibility = View.GONE

        viewModel.categories.observe(viewLifecycleOwner) { cats ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, cats.map { it.name })
            val autoComplete = binding.layoutCategory.editText as? AutoCompleteTextView
            if (autoComplete?.adapter == null) {
                autoComplete?.setAdapter(adapter)
            }
        }

        viewModel.fieldDefs.observe(viewLifecycleOwner) { defs ->
            if (defs != currentFieldDefs) {
                currentFieldDefs = defs
                renderDynamicFields(defs)
            }
        }

        // RFID scan button
        binding.btnScanRfid.setOnClickListener { viewModel.scanRfid() }

        // Barcode scan button
        binding.btnScanBarcode.setOnClickListener {
            viewModel.scanBarcode { result ->
                if (result != null) binding.editBarcode.setText(result)
                else showReaderUnavailableIfNeeded()
            }
        }

        viewModel.rfidScanResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is RfidScanResult.Success -> binding.editRfid.setText(result.epc)
                is RfidScanResult.NoTagDetected -> Snackbar.make(binding.root, R.string.error_rfid_not_detected, Snackbar.LENGTH_LONG).show()
                is RfidScanResult.MultipleTagsDetected -> Snackbar.make(binding.root, getString(R.string.error_rfid_multiple, result.count), Snackbar.LENGTH_LONG).show()
                RfidScanResult.ReaderUnavailable -> showReaderUnavailableIfNeeded()
                null -> {}
            }
            viewModel.consumeRfidScanResult()
        }

        // FR-09b: mandatory fields get inline errors, not a silent block
        binding.btnSaveLink.setOnClickListener { attemptSave() }

        viewModel.saveResult.observe(viewLifecycleOwner) { result -> handleSaveResult(result) }

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

    private fun renderDynamicFields(defs: List<FieldDefinitionEntity>) {
        val visibleDefs = defs.filter { it.showOnLinking }

        // Only rebuild if the field count or keys have changed
        if (dynamicFieldViews.size == visibleDefs.size &&
            dynamicFieldViews.keys == visibleDefs.map { it.key }.toSet()) {
            return
        }

        binding.dynamicFieldsContainer.removeAllViews()
        dynamicFieldViews.clear()

        visibleDefs.forEach { def ->
            if (def.type == "DROPDOWN") {
                val til = TextInputLayout(requireContext()).apply {
                    hint = def.label + if (def.mandatory) " *" else ""
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(0, 8, 0, 8)
                    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                }
                val options = def.dropdownOptionsCsv?.split(",")?.map { it.trim() } ?: emptyList()
                val autoComplete = AutoCompleteTextView(requireContext()).apply {
                    inputType = android.text.InputType.TYPE_NULL
                    setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options))
                }
                til.addView(autoComplete)
                binding.dynamicFieldsContainer.addView(til)
                dynamicFieldViews[def.key] = autoComplete
            } else {
                val til = TextInputLayout(requireContext()).apply {
                    hint = def.label + if (def.mandatory) " *" else ""
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(0, 8, 0, 8)
                }
                val edit = TextInputEditText(requireContext()).apply {
                    inputType = if (def.type == "NUMBER") android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                                else android.text.InputType.TYPE_CLASS_TEXT
                }
                til.addView(edit)
                binding.dynamicFieldsContainer.addView(til)
                dynamicFieldViews[def.key] = edit
            }
        }
    }

    private fun showReaderUnavailableIfNeeded() {
        Snackbar.make(binding.root, R.string.error_reader_unavailable, Snackbar.LENGTH_LONG).show()
    }

    private fun attemptSave() {
        clearFieldErrors()
        val name = binding.editName.text?.toString()?.trim().orEmpty()
        val barcode = binding.editBarcode.text?.toString()?.trim().orEmpty()
        val category = (binding.layoutCategory.editText?.text?.toString() ?: "").trim()
        val rfid = binding.editRfid.text?.toString()?.trim().orEmpty()

        if (name.isBlank() || barcode.isBlank() || category.isBlank() || rfid.isBlank()) {
            if (name.isBlank()) binding.layoutName.error = "Required"
            if (barcode.isBlank()) binding.layoutBarcode.error = "Required"
            if (category.isBlank()) binding.layoutCategory.error = "Required"
            if (rfid.isBlank()) binding.layoutRfid.error = "Required"
            return
        }

        // Check mandatory custom fields
        var hasMissingMandatory = false
        currentFieldDefs.filter { it.mandatory && it.showOnLinking }.forEach { def ->
            val view = dynamicFieldViews[def.key]
            val value = view?.text?.toString()?.trim().orEmpty()
            if (value.isBlank()) {
                (view?.parent as? TextInputLayout)?.error = "Required"
                hasMissingMandatory = true
            }
        }

        if (hasMissingMandatory) {
            return
        }

        val attributes = mutableMapOf<String, String>()
        dynamicFieldViews.forEach { (key, view) ->
            val value = view.text?.toString()?.trim().orEmpty()
            attributes[key] = value
            android.util.Log.d("IndividualLinking", "Custom field '$key' = '$value'")
        }
        android.util.Log.d("IndividualLinking", "Total custom fields collected: ${attributes.size}")

        viewModel.save(name, barcode, category, rfid, attributes)
    }

    private fun handleSaveResult(result: com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult?) {
        when (result) {
            is com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult.Success -> {
                Snackbar.make(binding.root, "Item saved", Snackbar.LENGTH_SHORT).show()
                clearForm()
                viewModel.consumeSaveResult()
            }
            is com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult.ValidationError -> {
                applyFieldErrors(result.fieldErrors)
                viewModel.consumeSaveResult()
            }
            is com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult.DuplicateRfid -> {
                binding.layoutRfid.error = getString(R.string.error_rfid_duplicate)
                viewModel.consumeSaveResult()
            }
            is com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult.DemoLimitReached -> {
                Snackbar.make(binding.root, getString(R.string.error_demo_items_limit, com.gigakin.stockbuddy.util.DemoLimits.MAX_ITEMS), Snackbar.LENGTH_LONG).show()
                viewModel.consumeSaveResult()
            }
            is com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult.DatabaseError -> {
                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                viewModel.consumeSaveResult()
            }
            null -> {
                // Result already consumed, do nothing
            }
        }
    }

    private fun applyFieldErrors(errors: Map<String, String>) {
        if (errors.containsKey("name")) binding.layoutName.error = "Required"
        if (errors.containsKey("barcode")) binding.layoutBarcode.error = "Required"
        if (errors.containsKey("category")) binding.layoutCategory.error = "Required"
        if (errors.containsKey("rfid")) binding.layoutRfid.error = "Required"

        // All domain-specific field errors are in the form "attr_fieldKey"
        dynamicFieldViews.forEach { (fieldKey, view) ->
            if (errors.containsKey("attr_$fieldKey")) {
                (view.parent as? TextInputLayout)?.error = errors["attr_$fieldKey"] ?: "Required"
            }
        }
    }

    private fun clearFieldErrors() {
        binding.layoutName.error = null; binding.layoutBarcode.error = null
        binding.layoutCategory.error = null; binding.layoutRfid.error = null
        dynamicFieldViews.values.forEach { (it.parent as? TextInputLayout)?.error = null }
    }

    private fun clearForm() {
        try {
            binding.editName.text?.clear()
            binding.editBarcode.text?.clear()
            binding.editRfid.text?.clear()
            (binding.layoutCategory.editText)?.text?.clear()

            // Clear dynamic fields (with defensive null checks)
            dynamicFieldViews.forEach { (_, view) ->
                view.text?.clear()
            }
        } catch (e: Exception) {
            // Log but don't crash if there's any issue clearing
            android.util.Log.e("IndividualLinking", "Error clearing form", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
