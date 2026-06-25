package com.gigakin.stockbuddy.ui.linking

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentBulkLinkingBinding
import com.gigakin.stockbuddy.util.ViewModelFactory

/** S08 — Bulk Linking. FR-16: mandatory columns Name/Barcode/RFID; Article ID optional column. */
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
            binding.tvSelectedFile.text = selectedUri?.lastPathSegment ?: ""
            binding.btnImport.isEnabled = selectedUri != null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBulkLinkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnChooseFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
            }
            filePicker.launch(intent)
        }

        binding.btnImport.setOnClickListener {
            selectedUri?.let { uri ->
                requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                    viewModel.importCsv(stream)
                }
            }
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.tvSummary.visibility = View.VISIBLE
                binding.tvSummary.text = getString(R.string.import_summary_format, result.inserted, result.updated, result.rejected) +
                    if (result.reasons.isNotEmpty()) "\n\n" + result.reasons.joinToString("\n") else ""
                viewModel.consumeResult()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
