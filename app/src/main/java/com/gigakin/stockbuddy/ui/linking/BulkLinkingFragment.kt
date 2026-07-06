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
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.databinding.FragmentBulkLinkingBinding
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/** S08 — Bulk Linking. FR-16: mandatory columns Name/Barcode/Category/RFID; custom fields optional. */
class BulkLinkingFragment : Fragment() {
    private var _binding: FragmentBulkLinkingBinding? = null
    private val binding get() = _binding!!
    private var selectedUri: android.net.Uri? = null

    private val app get() = requireActivity().application as StockBuddyApp
    private val viewModel: BulkLinkingViewModel by viewModels {
        ViewModelFactory { BulkLinkingViewModel(app.itemRepository, app.fieldConfigRepository) }
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

        // Reader status observer
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            val (colorRes, textRes) = when (status) {
                ReaderStatus.CONNECTED -> R.color.status_available to R.string.reader_connected
                ReaderStatus.NOT_CONNECTED -> R.color.status_excess to R.string.reader_not_connected
                ReaderStatus.NOT_AVAILABLE -> R.color.reader_not_available to R.string.reader_not_available
            }
            androidx.core.widget.ImageViewCompat.setImageTintList(
                binding.readerStatusIcon,
                androidx.core.content.res.ResourcesCompat.getColorStateList(requireContext().resources, colorRes, null)
            )
            binding.tvReaderStatus.text = getString(textRes)
        }

        // Upload zone click
        binding.uploadZone.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
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

        // Observe field definitions to populate CSV schema
        viewModel.fieldDefinitions.observe(viewLifecycleOwner) { fieldDefs ->
            populateSchemaTable(fieldDefs)
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
    }

    private fun populateSchemaTable(fieldDefs: List<FieldDefinitionEntity>) {
        // Clear existing rows
        binding.schemaHeaderRow.removeAllViews()
        binding.schemaDataContainer.removeAllViews()

        // Build header from system fields + custom fields
        val systemFields = listOf("Name", "Barcode", "Category", "RFID ID")
        val customFields = fieldDefs.filter { it.showOnCsv }

        // Header row
        (systemFields + customFields.map { it.label }).forEach { header ->
            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = header
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(requireContext().getColor(R.color.md_theme_onSurfaceVariant))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            binding.schemaHeaderRow.addView(tv)
        }

        // Example data row
        val exampleData = listOf("Rugged Case X1", "88291022", "Accessory", "E28011912...") +
            customFields.map { "" }

        val dataRow = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(requireContext().getColor(R.color.md_theme_surfaceContainerLowest))
            setPadding(12, 12, 12, 12)
        }

        exampleData.forEachIndexed { idx, value ->
            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = value
                textSize = 13f
                setTextColor(requireContext().getColor(R.color.md_theme_onSurfaceVariant))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            dataRow.addView(tv)
        }

        binding.schemaDataContainer.addView(dataRow)

        // Mandatory/optional indicator row
        val indicatorRow = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(requireContext().getColor(R.color.md_theme_surfaceContainerLowest))
            setPadding(12, 8, 12, 8)
            alpha = 0.6f
        }

        // All system fields are mandatory
        (systemFields.map { "Required" } + customFields.map { if (it.mandatory) "Required" else "Optional" }).forEach { indicator ->
            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = indicator
                textSize = 10f
                setTextColor(requireContext().getColor(R.color.md_theme_outline))
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            indicatorRow.addView(tv)
        }

        binding.schemaDataContainer.addView(indicatorRow)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
