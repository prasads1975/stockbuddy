package com.gigakin.stockbuddy.ui.fieldconfig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.databinding.DialogAddFieldBinding

class AddFieldDialogFragment(
    private val onFieldAdded: (FieldDefinitionEntity) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogAddFieldBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddFieldBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val types = listOf("Text", "Number", "Dropdown", "Date", "Currency")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        (binding.fieldTypeDropdown as? AutoCompleteTextView)?.setAdapter(typeAdapter)

        binding.fieldTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            if (types[position].equals("Dropdown", ignoreCase = true)) {
                binding.layoutDropdownValues.visibility = View.VISIBLE
            } else {
                binding.layoutDropdownValues.visibility = View.GONE
            }
        }

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val label = binding.editFieldLabel.text?.toString()?.trim().orEmpty()
            val type = binding.fieldTypeDropdown.text?.toString() ?: "Text"

            if (label.isBlank()) {
                binding.editFieldLabel.error = "Field label is required"
                return@setOnClickListener
            }

            val key = label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            val field = FieldDefinitionEntity(
                key = key,
                label = label,
                type = type,
                mandatory = binding.switchMandatory.isChecked,
                dropdownOptionsCsv = if (type.equals("Dropdown", ignoreCase = true)) {
                    binding.editDropdownValues.text?.toString()
                } else null
            )
            onFieldAdded(field)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
