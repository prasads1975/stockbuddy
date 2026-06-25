package com.gigakin.stockbuddy.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.FragmentInventoryEntryBinding
import kotlinx.coroutines.launch

/** S11 — Enter Inventory Code before starting a scan session (FR-29). */
class InventoryEntryFragment : Fragment() {
    private var _binding: FragmentInventoryEntryBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInventoryEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnStartSession.setOnClickListener {
            val code = binding.editCode.text?.toString()?.trim().orEmpty()
            if (code.isBlank()) { binding.layoutCode.error = "Required"; return@setOnClickListener }
            lifecycleScope.launch {
                val sessionId = app.inventoryRepository.startSession(code)
                findNavController().navigate(
                    InventoryEntryFragmentDirections.actionToScanning(sessionId, code)
                )
            }
        }
        binding.btnViewReports.setOnClickListener {
            findNavController().navigate(InventoryEntryFragmentDirections.actionToReportsList())
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
