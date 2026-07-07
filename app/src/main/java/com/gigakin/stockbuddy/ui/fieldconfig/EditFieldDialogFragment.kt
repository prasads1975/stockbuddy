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
import com.gigakin.stockbuddy.databinding.DialogEditFieldBinding
import com.gigakin.stockbuddy.util.FieldType

class EditFieldDialogFragment(
    private val field: FieldDefinitionEntity,
    private val onFieldUpdated: (FieldDefinitionEntity) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogEditFieldBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(
            field: FieldDefinitionEntity,
            onFieldUpdated: (FieldDefinitionEntity) -> Unit
        ) = EditFieldDialogFragment(field, onFieldUpdated)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditFieldBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val types = FieldType.labels   // Text, Number, Dropdown, Date (single source of truth)
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        (binding.fieldTypeDropdown as? AutoCompleteTextView)?.setAdapter(typeAdapter)

        binding.editFieldLabel.setText(field.label)
        // Show the display label for the stored canonical value (handles legacy "Number"/"Currency").
        binding.fieldTypeDropdown.setText(FieldType.labelForValue(field.type), false)
        binding.switchMandatory.isChecked = field.mandatory
        if (field.type.equals(FieldType.DROPDOWN_VALUE, ignoreCase = true)) {
            binding.layoutDropdownValues.visibility = View.VISIBLE
            binding.editDropdownValues.setText(field.dropdownOptionsCsv)
        }

        binding.fieldTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            if (types[position].equals(FieldType.DROPDOWN.label, ignoreCase = true)) {
                binding.layoutDropdownValues.visibility = View.VISIBLE
            } else {
                binding.layoutDropdownValues.visibility = View.GONE
            }
        }

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val label = binding.editFieldLabel.text?.toString()?.trim().orEmpty()
            // Store the canonical enum value ("NUMBER"), not the picker label ("Number").
            val type = FieldType.valueForLabel(binding.fieldTypeDropdown.text?.toString().orEmpty())

            if (label.isBlank()) {
                binding.editFieldLabel.error = "Field label is required"
                return@setOnClickListener
            }

            val updated = field.copy(
                label = label,
                type = type,
                mandatory = binding.switchMandatory.isChecked,
                dropdownOptionsCsv = if (type == FieldType.DROPDOWN_VALUE) {
                    binding.editDropdownValues.text?.toString()
                } else null
            )
            onFieldUpdated(updated)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
