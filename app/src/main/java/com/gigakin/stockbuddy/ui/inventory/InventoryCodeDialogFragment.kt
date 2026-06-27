package com.gigakin.stockbuddy.ui.inventory

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.DialogInventoryCodeBinding
import com.gigakin.stockbuddy.ui.home.HomeFragmentDirections
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "InventoryCodeDialog"

class InventoryCodeDialogFragment : DialogFragment() {
    private var _binding: DialogInventoryCodeBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogInventoryCodeBinding.inflate(LayoutInflater.from(requireContext()))

        binding.btnStartSession.setOnClickListener {
            val code = binding.editCode.text?.toString()?.trim().orEmpty()
            if (code.isBlank()) {
                binding.layoutCode.error = "Required"
                return@setOnClickListener
            }
            Log.d(TAG, "Start button clicked with code: '$code'")
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Launching coroutine to create session...")
                    val sessionId = withContext(Dispatchers.IO) {
                        Log.d(TAG, "On IO dispatcher, calling startSession...")
                        val id = app.inventoryRepository.startSession(code)
                        Log.d(TAG, "startSession returned: $id")
                        id
                    }
                    Log.d(TAG, "Session created successfully with ID: $sessionId")
                    dismiss()
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeToScanning(sessionId, code, autoStart = true)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating session", e)
                    binding.layoutCode.error = "Error: ${e.message}"
                }
            }
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(false)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
