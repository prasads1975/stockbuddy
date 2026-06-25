package com.gigakin.stockbuddy.ui.fieldconfig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gigakin.stockbuddy.R
import com.gigakin.stockbuddy.StockBuddyApp
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.repo.FieldConfigRepository
import com.gigakin.stockbuddy.databinding.FragmentFieldConfigBinding
import com.gigakin.stockbuddy.util.ArticleIdMode
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
        adapter = FieldDefAdapter { field -> viewModel.removeField(field) }
        binding.recyclerFields.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFields.adapter = adapter
        viewModel.fields.observe(viewLifecycleOwner) { adapter.submitList(it) }

        // FR-77l: starter templates — the live domain-switch demo moment
        binding.btnTemplateToyRetail.setOnClickListener { viewModel.applyTemplate(FieldConfigRepository.Templates.TOY_RETAIL) }
        binding.btnTemplateJewellery.setOnClickListener { viewModel.applyTemplate(FieldConfigRepository.Templates.JEWELLERY) }
        binding.btnTemplateGeneric.setOnClickListener { viewModel.applyTemplate(FieldConfigRepository.Templates.GENERIC) }

        // FR-77j: ad-hoc field add
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("TEXT", "NUMBER", "DROPDOWN", "DATE"))
        binding.spinnerNewFieldType.adapter = typeAdapter

        binding.btnAddField.setOnClickListener {
            val label = binding.editNewFieldLabel.text?.toString()?.trim().orEmpty()
            if (label.isBlank()) return@setOnClickListener
            val key = label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            viewModel.addField(
                FieldDefinitionEntity(
                    key = key, label = label,
                    type = binding.spinnerNewFieldType.selectedItem as String,
                    mandatory = binding.switchMandatory.isChecked
                )
            )
            binding.editNewFieldLabel.text?.clear()
            binding.switchMandatory.isChecked = false
        }

        // FR-77o / NFR-56: Article ID usage toggle, default Optional
        binding.radioGroupArticleId.check(when (viewModel.articleIdMode) {
            ArticleIdMode.NOT_USED -> R.id.radioNotUsed
            ArticleIdMode.OPTIONAL -> R.id.radioOptional
            ArticleIdMode.MANDATORY -> R.id.radioMandatory
        })
        binding.radioGroupArticleId.setOnCheckedChangeListener { _, checkedId ->
            viewModel.articleIdMode = when (checkedId) {
                R.id.radioNotUsed -> ArticleIdMode.NOT_USED
                R.id.radioMandatory -> ArticleIdMode.MANDATORY
                else -> ArticleIdMode.OPTIONAL
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
