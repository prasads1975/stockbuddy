package com.gigakin.stockbuddy.ui.export

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.repo.InventoryRepository
import com.gigakin.stockbuddy.databinding.FragmentExportBottomSheetBinding
import kotlinx.coroutines.launch

/**
 * S14 — Export Report modal. FR-49: Share and Download only for MVP (API/LAN channels,
 * FR-53-56, are out of scope per Section 2 of the MVP Scope doc).
 * Article ID is always included in exports (mandatory field).
 */
class ExportBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentExportBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private var sessionId: String = ""
    private var sessionCode: String = ""

    companion object {
        fun newInstance(sessionId: String, sessionCode: String) = ExportBottomSheetFragment().apply {
            arguments = Bundle().apply { putString("sessionId", sessionId); putString("sessionCode", sessionCode) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExportBottomSheetBinding.inflate(inflater, container, false)
        sessionId = requireArguments().getString("sessionId").orEmpty()
        sessionCode = requireArguments().getString("sessionCode").orEmpty()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvTitle.text = getString(R.string.export_title)

        binding.btnShare.setOnClickListener { lifecycleScope.launch { exportThen { file -> startActivity(android.content.Intent.createChooser(app.exportRepository.shareIntentFor(file), "Share report")) } } }
        binding.btnDownload.setOnClickListener { lifecycleScope.launch { exportThen { file ->
            Snackbar.make(binding.root, getString(R.string.download_success_format, app.exportRepository.downloadedFilePath(file)), Snackbar.LENGTH_LONG).show()
        } } }
    }

    private suspend fun exportThen(action: (java.io.File) -> Unit) {
        val results: List<InventoryRepository.ResultItem> = app.inventoryRepository.computeResults(sessionId, null)
        val fieldDefs = app.fieldConfigRepository.getFields()
        val rows = app.exportRepository.buildCsv(results, fieldDefs)
        val fileName = app.exportRepository.buildFileName(sessionCode)
        val file = app.exportRepository.writeCsvFile(fileName, rows)
        action(file)
        dismiss()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
