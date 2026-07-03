package com.gigakin.stockbuddy.ui.fieldconfig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.databinding.FragmentFieldConfigBinding
import com.gigakin.stockbuddy.util.ReaderStatus
import com.gigakin.stockbuddy.util.ViewModelFactory

/**
 * S19/S20 — Field Configuration: the headline domain-switching demo feature (FR-77i-o).
 * Reached directly from Settings (FR-80a), NOT gated behind a non-skippable wizard
 * (Section 1.1, MVP Scope doc — the wizard framing itself, FR-77, is cut from MVP).
 */
class FieldConfigFragment : Fragment() {
    private var _binding: FragmentFieldConfigBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as StockBuddyApp

    private val viewModel: FieldConfigViewModel by viewModels {
        ViewModelFactory { FieldConfigViewModel(app.fieldConfigRepository) }
    }
    private lateinit var adapter: FieldDefAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFieldConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Back button navigation to Settings
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Reader status observer
        app.scannerManager.status.observe(viewLifecycleOwner) { status ->
            val (colorRes, textRes) = when (status) {
                ReaderStatus.CONNECTED -> R.color.status_available to R.string.reader_connected
                ReaderStatus.NOT_CONNECTED -> R.color.status_excess to R.string.reader_not_connected
                ReaderStatus.NOT_AVAILABLE -> R.color.reader_not_available to R.string.reader_not_available
            }
            ImageViewCompat.setImageTintList(
                binding.readerStatusIcon,
                ResourcesCompat.getColorStateList(requireContext().resources, colorRes, null)
            )
            binding.tvReaderStatus.text = getString(textRes)
        }

        // Setup RecyclerView with edit/delete callbacks
        adapter = FieldDefAdapter(
            onEditClick = { field -> showEditDialog(field) },
            onDeleteClick = { field -> viewModel.removeField(field) }
        )
        binding.recyclerFields.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFields.adapter = adapter
        viewModel.fields.observe(viewLifecycleOwner) { adapter.submitList(it) }

        // Add Field button - shows dialog
        binding.btnAddField.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialog = AddFieldDialogFragment { field ->
            viewModel.addField(field)
        }
        dialog.show(childFragmentManager, "add_field")
    }

    private fun showEditDialog(field: FieldDefinitionEntity) {
        val dialog = EditFieldDialogFragment.newInstance(field) { updated ->
            viewModel.updateField(field, updated)
        }
        dialog.show(childFragmentManager, "edit_field")
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
