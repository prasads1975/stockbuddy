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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.databinding.FragmentIndividualLinkingBinding
import com.gigakin.stockbuddy.hardware.RfidScanResult
import com.gigakin.stockbuddy.util.ArticleIdMode
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/**
 * S06 — Manual Entry form (QR mode, FR-01-04, is cut from MVP; this is the only entry path).
 * Renders fixed fields + Article ID (mode-dependent, NFR-56) + domain-specific fields
 * dynamically (NFR-52). Barcode scan = compact inline icon (NFR-10f); RFID scan = prominent
 * labelled button (NFR-10f); Save Link = the one oversized thumb-zone primary action (NFR-10c/d).
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.legendRequired.text = getString(R.string.legend_required)

        // Article ID visibility/requirement follows the configured mode (Section 1.7, NFR-56)
        when (viewModel.articleIdMode) {
            ArticleIdMode.NOT_USED -> binding.layoutArticleId.visibility = View.GONE
            ArticleIdMode.OPTIONAL -> binding.layoutArticleId.hint = getString(R.string.field_article_id)
            ArticleIdMode.MANDATORY -> binding.layoutArticleId.hint = getString(R.string.field_article_id) + " *"
        }

        viewModel.categories.observe(viewLifecycleOwner) { cats ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, cats.map { it.name })
            (binding.layoutCategory.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        }

        viewModel.fieldDefs.observe(viewLifecycleOwner) { defs ->
            currentFieldDefs = defs
            renderDynamicFields(defs)
        }

        // NFR-10f: Barcode scan = compact inline icon, adjacent to the field
        binding.layoutBarcode.setEndIconOnClickListener {
            viewModel.scanBarcode { result ->
                if (result != null) binding.editBarcode.setText(result)
                else showReaderUnavailableIfNeeded()
            }
        }

        // NFR-10f: RFID scan = prominent labelled button, distinct from Save Link
        binding.btnScanRfid.setOnClickListener { viewModel.scanRfid() }

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
    }

    private fun renderDynamicFields(defs: List<FieldDefinitionEntity>) {
        binding.dynamicFieldsContainer.removeAllViews()
        dynamicFieldViews.clear()
        defs.filter { it.showOnLinking }.forEach { def ->
            if (def.type == "DROPDOWN") {
                // FR-77i/NFR-52: DROPDOWN fields render as ExposedDropdownMenu (spinner-like)
                val til = TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputFilledStyle).apply {
                    hint = def.label + if (def.mandatory) " *" else ""
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(0, 8, 0, 8)
                    setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED)
                }
                val options = def.dropdownOptionsCsv?.split(",")?.map { it.trim() } ?: emptyList()
                val autoComplete = AutoCompleteTextView(requireContext()).apply {
                    inputType = android.text.InputType.TYPE_NULL  // Prevent keyboard; dropdown only
                    setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options))
                }
                til.addView(autoComplete)
                binding.dynamicFieldsContainer.addView(til)
                dynamicFieldViews[def.key] = autoComplete
            } else {
                // TEXT, NUMBER, DATE types: standard TextInputEditText
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
        val articleId = binding.editArticleId.text?.toString()?.trim()
        val attributes = dynamicFieldViews.mapValues { (_, v) -> v.text?.toString()?.trim().orEmpty() }

        viewModel.save(name, barcode, category, rfid, articleId, attributes)
    }

    private fun handleSaveResult(result: com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult?) {
        when (result) {
            is com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult.Success -> {
                Snackbar.make(binding.root, "Item saved", Snackbar.LENGTH_SHORT).show()
                clearForm()
            }
            is com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult.ValidationError -> {
                applyFieldErrors(result.fieldErrors)
            }
            is com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult.DuplicateRfid -> {
                binding.layoutRfid.error = getString(R.string.error_rfid_duplicate)
            }
            is com.gigakin.stockbuddy.data.repo.ItemRepository.SaveResult.DemoLimitReached -> {
                Snackbar.make(binding.root, getString(R.string.error_demo_items_limit, com.gigakin.stockbuddy.util.DemoLimits.MAX_ITEMS), Snackbar.LENGTH_LONG).show()
            }
            null -> {}
        }
        viewModel.consumeSaveResult()
    }

    private fun applyFieldErrors(errors: Map<String, String>) {
        if (errors.containsKey("name")) binding.layoutName.error = "Required"
        if (errors.containsKey("barcode")) binding.layoutBarcode.error = "Required"
        if (errors.containsKey("category")) binding.layoutCategory.error = "Required"
        if (errors.containsKey("rfid")) binding.layoutRfid.error = "Required"
        if (errors.containsKey("articleId")) binding.layoutArticleId.error = "Required"
        errors.keys.filter { it.startsWith("attr_") }.forEach { key ->
            val fieldKey = key.removePrefix("attr_")
            // Find the TextInputLayout parent of the matching dynamic field to show an error.
            (dynamicFieldViews[fieldKey]?.parent as? TextInputLayout)?.error = "Required"
        }
    }

    private fun clearFieldErrors() {
        binding.layoutName.error = null; binding.layoutBarcode.error = null
        binding.layoutCategory.error = null; binding.layoutRfid.error = null
        binding.layoutArticleId.error = null
        dynamicFieldViews.values.forEach { (it.parent as? TextInputLayout)?.error = null }
    }

    private fun clearForm() {
        binding.editName.text?.clear(); binding.editBarcode.text?.clear()
        (binding.layoutCategory.editText)?.text?.clear()
        binding.editRfid.text?.clear(); binding.editArticleId.text?.clear()
        dynamicFieldViews.values.forEach { it.text?.clear() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
