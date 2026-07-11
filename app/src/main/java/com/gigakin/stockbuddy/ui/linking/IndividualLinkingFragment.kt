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
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.databinding.FragmentIndividualLinkingBinding
import com.gigakin.stockbuddy.hardware.RfidScanResult
import com.gigakin.stockbuddy.util.QrPayloadParser
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/**
 * S06 — Individual Linking form. Reached either from Linking Options (Manual) or from the
 * QR Code Linking screen (S07) with the fields pre-filled from the scanned payload (FR-02/03).
 * Renders fixed fields (name, barcode, category, RFID) + all domain-specific fields dynamically
 * from field_definitions (NFR-52). Fields use a label-above layout; RFID/Barcode scan are navy
 * square icon buttons; Save Link = thumb-zone primary action (NFR-10c/d). When arrived via QR
 * (fromQr), a successful save returns to the scanner for the next item (FR-11).
 */
class IndividualLinkingFragment : Fragment() {
    private var _binding: FragmentIndividualLinkingBinding? = null
    private val binding get() = _binding!!

    private val app get() = requireActivity().application as StockBuddyApp
    private val args: IndividualLinkingFragmentArgs by navArgs()
    private val viewModel: IndividualLinkingViewModel by viewModels {
        ViewModelFactory {
            IndividualLinkingViewModel(app.itemRepository, app.fieldConfigRepository, app.categoryRepository, app.scannerManager)
        }
    }

    // QR-prefilled domain attribute values, applied once the dynamic fields are rendered.
    private var pendingAttributes: Map<String, String>? = null

    private val dynamicFieldViews = mutableMapOf<String, EditText>()
    // Key → the field's TextInputLayout, so we can show errors on it. (editText.parent is the
    // TextInputLayout's internal frame, NOT the TextInputLayout — casting it fails silently.)
    private val dynamicFieldLayouts = mutableMapOf<String, TextInputLayout>()
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

        applyPrefill()
    }

    /** FR-02/03: pre-fill fixed fields from the QR payload; dynamic-field values are applied in
     *  renderDynamicFields() once those views exist. No-op for the Manual path (all args null). */
    private fun applyPrefill() {
        args.prefillProductName?.let { binding.editName.setText(it) }
        args.prefillBarcode?.let { binding.editBarcode.setText(it) }
        args.prefillRfid?.let { binding.editRfid.setText(it) }
        args.prefillCategory?.let {
            (binding.layoutCategory.editText as? AutoCompleteTextView)?.setText(it, false)
        }
        val attrs = QrPayloadParser.attributesFromJson(args.prefillAttributesJson)
        if (attrs.isNotEmpty()) pendingAttributes = attrs
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
        dynamicFieldLayouts.clear()

        val inflater = LayoutInflater.from(requireContext())
        visibleDefs.forEach { def ->
            val isDropdown = def.type == "DROPDOWN"
            // Inflate the same outlined-box template the fixed fields use, so rendering is identical.
            val layoutRes = if (isDropdown) R.layout.item_linking_dynamic_dropdown else R.layout.item_linking_dynamic_text
            val fieldView = inflater.inflate(layoutRes, binding.dynamicFieldsContainer, false)

            fieldView.findViewById<android.widget.TextView>(R.id.fieldLabel).text =
                def.label + if (def.mandatory) " *" else ""

            val input = fieldView.findViewById<EditText>(R.id.fieldInput)
            if (isDropdown) {
                val options = def.dropdownOptionsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                (input as AutoCompleteTextView).apply {
                    keyListener = null
                    setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options))
                }
            } else {
                input.inputType = if (def.type == "NUMBER")
                    android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                else android.text.InputType.TYPE_CLASS_TEXT
            }

            binding.dynamicFieldsContainer.addView(fieldView)
            dynamicFieldViews[def.key] = input
            dynamicFieldLayouts[def.key] = fieldView.findViewById(R.id.fieldInputLayout)
        }

        // Apply any QR-prefilled attribute values now that the dynamic fields exist (once).
        pendingAttributes?.let { attrs ->
            val invalidLabels = mutableListOf<String>()
            attrs.forEach { (key, value) ->
                val def = visibleDefs.find { it.key == key } ?: return@forEach
                if (def.type == "DROPDOWN") {
                    // Dropdown fields are pick-only in the UI (keyListener = null), so a QR value
                    // must match one of the configured options exactly (case-insensitive) or it's
                    // left blank rather than silently injecting an out-of-list value.
                    val options = def.dropdownOptionsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    val match = options.firstOrNull { it.equals(value, ignoreCase = true) }
                    if (match != null) {
                        dynamicFieldViews[key]?.setText(match)
                    } else if (value.isNotBlank()) {
                        invalidLabels.add(def.label)
                    }
                } else {
                    dynamicFieldViews[key]?.setText(value)
                }
            }
            if (invalidLabels.isNotEmpty()) {
                Snackbar.make(binding.root, getString(R.string.qr_invalid_dropdown_values, invalidLabels.joinToString(", ")), Snackbar.LENGTH_LONG).show()
            }
            pendingAttributes = null
        }
    }

    private fun showReaderUnavailableIfNeeded() {
        Snackbar.make(binding.root, R.string.error_reader_unavailable, Snackbar.LENGTH_LONG).show()
    }

    /** Centered, auto-dismissing "Item linked successfully" confirmation (no screen dimming). */
    private fun showLinkedSuccess() {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.view_success_overlay)
        dialog.setCancelable(true)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setDimAmount(0f)   // don't dim the screen behind it
        }
        dialog.show()
        binding.root.postDelayed({
            if (isAdded && dialog.isShowing) dialog.dismiss()
        }, 1500)
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
            val value = dynamicFieldViews[def.key]?.text?.toString()?.trim().orEmpty()
            if (value.isBlank()) {
                dynamicFieldLayouts[def.key]?.error = "Required"
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
                viewModel.consumeSaveResult()
                if (args.fromQr) {
                    // FR-11: return to the scanner (re-armed on its onStart) for the next item.
                    android.widget.Toast.makeText(requireContext(), R.string.item_linked_success, android.widget.Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack(R.id.qrLinkingFragment, false)
                } else {
                    showLinkedSuccess()
                    clearForm()
                }
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
        // Show the repository's actual message (e.g. "Category doesn't exist — add it in Settings
        // first", "Barcode already exists as 'X'"), not a flattened "Required".
        errors["name"]?.let { binding.layoutName.error = it }
        errors["barcode"]?.let { binding.layoutBarcode.error = it }
        errors["category"]?.let { binding.layoutCategory.error = it }
        errors["rfid"]?.let { binding.layoutRfid.error = it }

        // All domain-specific field errors are in the form "attr_fieldKey"
        dynamicFieldLayouts.forEach { (fieldKey, layout) ->
            errors["attr_$fieldKey"]?.let { layout.error = it }
        }
    }

    private fun clearFieldErrors() {
        binding.layoutName.error = null; binding.layoutBarcode.error = null
        binding.layoutCategory.error = null; binding.layoutRfid.error = null
        dynamicFieldLayouts.values.forEach { it.error = null }
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
