package com.gigakin.stockbuddy.ui.linking

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.databinding.FragmentBulkLinkingBinding
import com.gigakin.stockbuddy.util.ReaderStatusBar
import com.gigakin.stockbuddy.util.ViewModelFactory

/** S08 — Bulk Linking. FR-16: mandatory columns Name/Barcode/Category/RFID; custom fields optional. */
class BulkLinkingFragment : Fragment() {
    private var _binding: FragmentBulkLinkingBinding? = null
    private val binding get() = _binding!!
    private var selectedUri: android.net.Uri? = null

    private val app get() = requireActivity().application as StockBuddyApp
    private val viewModel: BulkLinkingViewModel by viewModels {
        ViewModelFactory { BulkLinkingViewModel(app.itemRepository, app.fieldConfigRepository, app.exportRepository) }
    }

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedUri = result.data?.data
            binding.tvUploadTitle.text = selectedUri?.lastPathSegment ?: getString(R.string.bulk_linking_select_csv)
            binding.tvUploadSubtitle.text = getString(R.string.bulk_linking_file_ready)
            binding.btnImport.isEnabled = selectedUri != null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBulkLinkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        // Reader status observer (FR-81) — centralised in ReaderStatusBar.
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            ReaderStatusBar.bind(status, binding.readerStatusBar, binding.readerStatusIcon, binding.tvReaderStatus)
        }

        // Upload zone click — opens the Android system document picker (SAF).
        binding.uploadZone.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                // Broad base type + explicit CSV MIME variants so a valid .csv is never greyed out
                // (file managers/providers report CSVs under several different MIME types).
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "text/plain",
                    "application/vnd.ms-excel",
                    "application/octet-stream"
                ))
            }
            filePicker.launch(intent)
        }

        // Import button click
        binding.btnImport.setOnClickListener {
            selectedUri?.let { uri ->
                android.util.Log.d("BulkLinking", "Starting import for URI: $uri")
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    android.util.Log.d("BulkLinking", "Successfully opened input stream")
                    viewModel.importCsv(inputStream)
                } else {
                    android.util.Log.e("BulkLinking", "Failed to open input stream for URI: $uri")
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "Failed to open file. Check file permissions.",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Import result observer
        viewModel.result.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.summaryCard.visibility = View.VISIBLE
                binding.tvSummary.text = getString(R.string.import_summary_format, result.inserted, result.skipped, result.rejected) +
                    if (result.reasons.isNotEmpty()) "\n\n" + result.reasons.joinToString("\n") else ""
                viewModel.consumeResult()
            }
        }

        // Append configured domain-specific (custom) fields as extra rows in the schema table,
        // in the same style as the four fixed rows, so admins see the full expected CSV shape.
        viewModel.fieldDefinitions.observe(viewLifecycleOwner) { fieldDefs ->
            populateCustomSchemaRows(fieldDefs.filter { it.showOnCsv })
        }

        // Download Template — a ready-to-fill CSV matching the Expected CSV Schema above.
        binding.btnDownloadTemplate.setOnClickListener { viewModel.downloadTemplate() }

        viewModel.templateResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is BulkLinkingViewModel.TemplateDownloadResult.Success -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.download_success))
                        .setMessage(getString(R.string.download_location_format, result.location))
                        .setPositiveButton(getString(R.string.action_ok), null)
                        .show()
                    viewModel.consumeTemplateResult()
                }
                BulkLinkingViewModel.TemplateDownloadResult.Failure -> {
                    Snackbar.make(binding.root, getString(R.string.download_failed), Snackbar.LENGTH_LONG).show()
                    viewModel.consumeTemplateResult()
                }
                null -> {}
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun exampleTextFor(field: FieldDefinitionEntity): String = when (field.type) {
        "NUMBER" -> "19.99"
        "DATE" -> "2026-01-15"
        "DROPDOWN" -> field.dropdownOptionsCsv?.split(",")?.map { it.trim() }?.firstOrNull { it.isNotEmpty() } ?: "Option"
        else -> "Sample text"
    }

    private fun populateCustomSchemaRows(customFields: List<FieldDefinitionEntity>) {
        binding.customSchemaRows.removeAllViews()
        customFields.forEach { field ->
            binding.customSchemaRows.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                setBackgroundColor(requireContext().getColor(R.color.md_theme_outlineVariant))
            })

            val row = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(requireContext().getColor(android.R.color.white))
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            fun cell(text: String) = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                this.text = text
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.md_theme_onSurface))
            }
            row.addView(cell(field.label + if (field.mandatory) " *" else ""))
            row.addView(cell(exampleTextFor(field)))
            binding.customSchemaRows.addView(row)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
