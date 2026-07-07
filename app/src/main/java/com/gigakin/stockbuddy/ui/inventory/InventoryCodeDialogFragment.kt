package com.gigakin.stockbuddy.ui.inventory

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.databinding.DialogInventoryCodeBinding
import com.gigakin.stockbuddy.ui.home.HomeFragmentDirections
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class InventoryCodeDialogFragment : DialogFragment() {
    private var _binding: DialogInventoryCodeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogInventoryCodeBinding.inflate(LayoutInflater.from(requireContext()))

        binding.btnStartSession.setOnClickListener {
            val code = binding.editCode.text?.toString()?.trim().orEmpty()
            if (code.isBlank()) {
                binding.layoutCode.error = "Required"
                return@setOnClickListener
            }
            dismiss()
            findNavController().navigate(
                HomeFragmentDirections.actionHomeToInventoryStart(code)
            )
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Create the dialog with Material Design styling (scrim + rounded corners)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(true)  // Allow dismissing via back or outside tap
            .create()

        // Apply Material Design dialog window styling
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
